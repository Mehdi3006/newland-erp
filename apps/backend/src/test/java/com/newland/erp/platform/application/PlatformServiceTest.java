package com.newland.erp.platform.application;

import com.newland.erp.platform.domain.OutboxStatus;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

final class PlatformServiceTest {
    @Test
    void publishesEventThroughBusAndPersistsOutboxMessage() {
        final InMemoryPlatformRepository repository = new InMemoryPlatformRepository();
        final AtomicInteger published = new AtomicInteger();
        final PlatformService service = new PlatformService(repository, event -> published.incrementAndGet(),
                Clock.fixed(Instant.parse("2026-07-20T00:00:00Z"), ZoneOffset.UTC));

        final var message = service.publishEvent(new PlatformCommands.PublishEvent("identity", "UserCreated",
                UUID.randomUUID(), Map.of("username", "owner")));

        assertThat(published).hasValue(1);
        assertThat(message.status()).isEqualTo(OutboxStatus.PENDING);
        assertThat(repository.listPendingOutboxMessages(10)).containsExactly(message);
    }
}
