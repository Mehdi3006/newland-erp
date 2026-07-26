package com.newland.erp.platform.application;

import com.newland.erp.platform.domain.OutboxMessage;
import com.newland.erp.platform.application.integration.PlatformOutboxConsumer;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Durably delivers committed outbox records using leased, retryable claims. */
@Service
public final class OutboxDispatcher {
    private static final Duration CLAIM_LEASE = Duration.ofMinutes(2);
    private static final Duration MAX_RETRY_DELAY = Duration.ofHours(1);
    private static final int BATCH_SIZE = 100;
    private final PlatformRepository repository;
    private final DomainEventBus eventBus;
    private final List<PlatformOutboxConsumer> consumers;
    private final TransactionTemplate transactions;
    private final Clock clock;

    public OutboxDispatcher(final PlatformRepository platformRepository, final DomainEventBus bus,
                            final List<PlatformOutboxConsumer> outboxConsumers,
                            final PlatformTransactionManager transactionManager, final Clock systemClock) {
        repository = platformRepository;
        eventBus = bus;
        consumers = List.copyOf(outboxConsumers);
        transactions = new TransactionTemplate(transactionManager);
        clock = systemClock;
    }

    @Scheduled(fixedDelayString = "${newland.platform.outbox.dispatch-delay:1000}")
    public void dispatchScheduled() {
        dispatchBatch(BATCH_SIZE);
    }

    public int dispatchBatch(final int limit) {
        final Instant claimedAt = Instant.now(clock);
        final List<OutboxMessage> claimed = transactions.execute(status ->
                repository.claimOutboxMessages(limit, claimedAt, claimedAt.plus(CLAIM_LEASE)));
        if (claimed == null) {
            return 0;
        }
        for (final OutboxMessage message : claimed) {
            dispatch(message);
        }
        return claimed.size();
    }

    private void dispatch(final OutboxMessage message) {
        try {
            final PlatformOutboxConsumer.OutboxEvent event =
                    new PlatformOutboxConsumer.OutboxEvent(
                            message.event().eventId(),
                            message.event().sourceContext(),
                            message.event().eventType(),
                            message.event().aggregateId(),
                            message.event().occurredAt(),
                            message.event().payload());
            consumers.stream()
                    .filter(consumer -> consumer.supports(event.sourceContext(), event.eventType()))
                    .forEach(consumer -> consumer.consume(event));
            eventBus.publish(message.event());
            transactions.executeWithoutResult(status -> repository.markOutboxPublished(
                    message.id(), message.attempts(), Instant.now(clock)));
        } catch (RuntimeException exception) {
            final Instant now = Instant.now(clock);
            final Duration delay = retryDelay(message.attempts());
            final String error = safeError(exception);
            transactions.executeWithoutResult(status -> repository.markOutboxFailed(
                    message.id(), message.attempts(), now.plus(delay), error));
        }
    }

    private static Duration retryDelay(final int attempts) {
        final int exponent = Math.min(Math.max(attempts - 1, 0), 10);
        final Duration delay = Duration.ofSeconds(1L << exponent);
        return delay.compareTo(MAX_RETRY_DELAY) > 0 ? MAX_RETRY_DELAY : delay;
    }

    private static String safeError(final RuntimeException exception) {
        final String message = exception.getMessage() == null
                ? exception.getClass().getSimpleName() : exception.getMessage();
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }
}
