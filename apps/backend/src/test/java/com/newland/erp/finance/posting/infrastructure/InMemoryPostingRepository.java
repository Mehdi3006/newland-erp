package com.newland.erp.finance.posting.infrastructure;

import com.newland.erp.finance.posting.application.PostingPorts;
import com.newland.erp.finance.posting.domain.AccountingEvent;
import com.newland.erp.finance.posting.domain.PostingException;
import com.newland.erp.finance.posting.domain.PostingRequest;
import com.newland.erp.finance.posting.domain.PostingRule;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class InMemoryPostingRepository
    implements PostingPorts.EventRepository, PostingPorts.RuleRepository {
  private final ConcurrentHashMap<UUID, AccountingEvent> eventStore = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<UUID, PostingRequest> requestStore = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, UUID> idempotency = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<UUID, PostingRule> ruleStore = new ConcurrentHashMap<>();

  public boolean eventIdExists(final UUID id) {
    return eventStore.containsKey(id);
  }

  public Optional<AccountingEvent> findEvent(final UUID id) {
    return Optional.ofNullable(eventStore.get(id));
  }

  public Optional<AccountingEvent> findByIdempotencyKey(final String key) {
    return Optional.ofNullable(idempotency.get(key)).flatMap(this::findEvent);
  }

  public AccountingEvent saveEvent(final AccountingEvent event) {
    eventStore.putIfAbsent(event.eventId(), event);
    idempotency.putIfAbsent(event.idempotencyKey(), event.eventId());
    return event;
  }

  public PostingRequest saveRequest(final PostingRequest request) {
    requestStore.putIfAbsent(request.postingRequestId(), request);
    return request;
  }

  public Optional<PostingRequest> findRequest(final UUID id) {
    return Optional.ofNullable(requestStore.get(id));
  }

  public Optional<PostingRequest> findRequestByEvent(final UUID id) {
    return requestStore.values().stream()
        .filter(request -> request.accountingEventId().equals(id))
        .findFirst();
  }

  public PostingRequest updateRequest(final PostingRequest request) {
    requestStore.put(request.postingRequestId(), request);
    return request;
  }

  public synchronized Optional<PostingRequest> claimRequest(
      final UUID requestId, final int expectedVersion) {
    final PostingRequest current = requestStore.get(requestId);
    if (current == null
        || current.version() != expectedVersion
        || (current.status() != PostingRequest.Status.RECEIVED
            && current.status() != PostingRequest.Status.FAILED
            && current.status() != PostingRequest.Status.VALIDATING)) {
      return Optional.empty();
    }
    final PostingRequest claimed =
        new PostingRequest(
            current.postingRequestId(),
            current.accountingEventId(),
            PostingRequest.Status.VALIDATING,
            current.resolvedPostingRuleId(),
            current.resolvedPostingRuleVersion(),
            current.journalEntryId(),
            null,
            null,
            current.attempts() + 1,
            current.createdAt(),
            current.updatedAt(),
            current.version() + 1);
    requestStore.put(requestId, claimed);
    return Optional.of(claimed);
  }

  public synchronized void lockEvent(final UUID eventId) {
    if (eventId == null) {
      throw new IllegalArgumentException("Event ID is required.");
    }
  }

  public List<PostingRule> findApplicable(
      final String eventType, final UUID companyId, final LocalDate date) {
    final List<PostingRule> matches =
        ruleStore.values().stream()
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
            .sorted(java.util.Comparator.comparingInt(PostingRule::priority).reversed())
            .toList();
    final boolean hasCompanyRule =
        matches.stream().anyMatch(rule -> companyId.equals(rule.companyId()));
    return hasCompanyRule
        ? matches.stream().filter(rule -> companyId.equals(rule.companyId())).toList()
        : matches.stream().filter(rule -> rule.companyId() == null).toList();
  }

  public PostingRule save(final PostingRule rule) {
    if (ruleStore.putIfAbsent(rule.postingRuleId(), rule) != null) {
      throw new PostingException("Posting rule versions are immutable.");
    }
    return rule;
  }

  public synchronized PostingRule transition(
      final PostingRule rule, final PostingRule.Status expectedStatus) {
    final PostingRule current = ruleStore.get(rule.postingRuleId());
    if (current == null || current.status() != expectedStatus) {
      throw new PostingException("Posting rule was modified concurrently.");
    }
    ruleStore.put(rule.postingRuleId(), rule);
    return rule;
  }

  public Optional<PostingRule> findRule(final UUID postingRuleId) {
    return Optional.ofNullable(ruleStore.get(postingRuleId));
  }

  public Optional<PostingRule> findLatest(final String code, final UUID companyId) {
    return ruleStore.values().stream()
        .filter(rule -> rule.code().equals(code))
        .filter(rule -> java.util.Objects.equals(rule.companyId(), companyId))
        .max(java.util.Comparator.comparingInt(PostingRule::version));
  }

  public synchronized void lockActivationScope(final PostingRule candidate) {
    if (candidate == null) {
      throw new IllegalArgumentException("Posting rule is required.");
    }
  }

  public List<PostingRule> findActivationConflicts(final PostingRule candidate) {
    return ruleStore.values().stream()
        .filter(rule -> rule.status() == PostingRule.Status.ACTIVE)
        .filter(rule -> !rule.postingRuleId().equals(candidate.postingRuleId()))
        .filter(rule -> rule.conflictsWith(candidate))
        .toList();
  }

  public List<PostingRule> list() {
    return List.copyOf(ruleStore.values());
  }

  public List<PostingRule> list(final UUID companyId) {
    return ruleStore.values().stream()
        .filter(rule -> java.util.Objects.equals(rule.companyId(), companyId))
        .sorted(
            java.util.Comparator.comparing(PostingRule::code)
                .thenComparing(
                    PostingRule::version, java.util.Comparator.reverseOrder()))
        .toList();
  }
}
