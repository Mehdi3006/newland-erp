package com.newland.erp.finance.posting.infrastructure;

import com.newland.erp.finance.posting.application.PostingPorts;
import com.newland.erp.finance.posting.domain.AccountingEvent;
import com.newland.erp.finance.posting.domain.PostingRequest;
import com.newland.erp.finance.posting.domain.PostingRule;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public final class InMemoryPostingRepository
    implements PostingPorts.EventRepository, PostingPorts.RuleRepository {
  private final ConcurrentHashMap<UUID, AccountingEvent> eventStore = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<UUID, PostingRequest> requestStore = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, UUID> idempotency = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<UUID, PostingRule> ruleStore = new ConcurrentHashMap<>();

  @Override
  public boolean eventIdExists(final UUID id) {
    return eventStore.containsKey(id);
  }

  @Override
  public Optional<AccountingEvent> findEvent(final UUID id) {
    return Optional.ofNullable(eventStore.get(id));
  }

  @Override
  public Optional<AccountingEvent> findByIdempotencyKey(final String key) {
    return Optional.ofNullable(idempotency.get(key)).flatMap(this::findEvent);
  }

  @Override
  public AccountingEvent saveEvent(final AccountingEvent event) {
    eventStore.putIfAbsent(event.eventId(), event);
    idempotency.putIfAbsent(event.idempotencyKey(), event.eventId());
    return event;
  }

  @Override
  public PostingRequest saveRequest(final PostingRequest request) {
    requestStore.putIfAbsent(request.postingRequestId(), request);
    return request;
  }

  @Override
  public Optional<PostingRequest> findRequest(final UUID id) {
    return Optional.ofNullable(requestStore.get(id));
  }

  @Override
  public Optional<PostingRequest> findRequestByEvent(final UUID id) {
    return requestStore.values().stream()
        .filter(request -> request.accountingEventId().equals(id))
        .findFirst();
  }

  @Override
  public PostingRequest updateRequest(final PostingRequest request) {
    requestStore.put(request.postingRequestId(), request);
    return request;
  }

  @Override
  public List<PostingRule> findApplicable(
      final String eventType, final UUID companyId, final LocalDate date) {
    return ruleStore.values().stream()
        .filter(
            rule ->
                rule.appliesTo(
                    new AccountingEvent(
                        UUID.randomUUID(),
                        "lookup",
                        eventType,
                        "finance",
                        "lookup",
                        UUID.randomUUID(),
                        "lookup",
                        companyId,
                        UUID.randomUUID(),
                        date,
                        date,
                        "USD",
                        java.math.BigDecimal.ONE,
                        java.math.BigDecimal.ZERO,
                        java.math.BigDecimal.ZERO,
                        java.math.BigDecimal.ZERO,
                        "lookup",
                        java.util.Map.of(),
                        java.util.Map.of(),
                        java.time.Instant.now(),
                        "system",
                        1)))
        .toList();
  }

  @Override
  public PostingRule save(final PostingRule rule) {
    ruleStore.put(rule.postingRuleId(), rule);
    return rule;
  }

  @Override
  public List<PostingRule> list() {
    return List.copyOf(ruleStore.values());
  }
}
