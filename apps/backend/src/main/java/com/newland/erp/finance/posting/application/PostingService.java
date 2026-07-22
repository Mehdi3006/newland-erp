package com.newland.erp.finance.posting.application;

import com.newland.erp.finance.posting.domain.AccountingEvent;
import com.newland.erp.finance.posting.domain.PostingException;
import com.newland.erp.finance.posting.domain.PostingRequest;
import com.newland.erp.finance.posting.domain.PostingResult;
import com.newland.erp.finance.posting.domain.PostingRule;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public final class PostingService implements FinancialPostingPort {
  private final PostingPorts.EventRepository events;
  private final PostingPorts.RuleRepository rules;
  private final PostingPorts.CompanyValidationPort companies;
  private final PostingPorts.BranchValidationPort branches;
  private final PostingPorts.CurrencyValidationPort currencies;
  private final PostingPorts.ExchangeRateValidationPort rates;
  private final PostingPorts.AccountingPeriodPort periods;
  private final PostingPorts.FinancialDimensionValidationPort dimensions;
  private final PostingPorts.JournalPostingPort journals;
  private final PostingPorts.AuditPort audit;
  private final PostingPorts.TransactionalOutboxPort outbox;
  private final Clock clock;

  public PostingService(
      final PostingPorts.EventRepository eventRepository,
      final PostingPorts.RuleRepository ruleRepository,
      final PostingPorts.CompanyValidationPort companyPort,
      final PostingPorts.BranchValidationPort branchPort,
      final PostingPorts.CurrencyValidationPort currencyPort,
      final PostingPorts.ExchangeRateValidationPort ratePort,
      final PostingPorts.AccountingPeriodPort periodPort,
      final PostingPorts.FinancialDimensionValidationPort dimensionPort,
      final PostingPorts.JournalPostingPort journalPort,
      final PostingPorts.AuditPort auditPort,
      final PostingPorts.TransactionalOutboxPort outboxPort,
      final Clock systemClock) {
    events = eventRepository;
    rules = ruleRepository;
    companies = companyPort;
    branches = branchPort;
    currencies = currencyPort;
    rates = ratePort;
    periods = periodPort;
    dimensions = dimensionPort;
    journals = journalPort;
    audit = auditPort;
    outbox = outboxPort;
    clock = systemClock;
  }

  @Override
  @Transactional
  public PostingResult submit(final AccountingEvent event) {
    final var existing = events.findByIdempotencyKey(event.idempotencyKey());
    if (existing.isPresent()) {
      return resultFor(events.findRequestByEvent(existing.get().eventId()).orElseThrow());
    }
    if (events.eventIdExists(event.eventId())) {
      throw new PostingException("Duplicate accounting event ID.");
    }
    validateEvent(event);
    events.saveEvent(event);
    final PostingRequest request =
        new PostingRequest(
            UUID.randomUUID(),
            event.eventId(),
            PostingRequest.Status.RECEIVED,
            null,
            null,
            null,
            null,
            null,
            0,
            Instant.now(clock),
            Instant.now(clock),
            0);
    events.saveRequest(request);
    audit.record(event.submittedBy(), "ACCOUNTING_EVENT_RECEIVED", event.eventId());
    outbox.publish("FinanceAccountingEventAccepted", event.eventId());
    return execute(request, event);
  }

  @Override
  @Transactional(readOnly = true)
  public PostingResult preview(final AccountingEvent event) {
    validateEvent(event);
    final PostingRule rule = resolve(event);
    audit.record(event.submittedBy(), "POSTING_PREVIEWED", event.eventId());
    return new PostingResult(
        null, event.eventId(), PostingRequest.Status.RULE_RESOLVED, null, null, null, rule.code());
  }

  @Override
  public PostingRequest status(final UUID postingRequestId) {
    return events
        .findRequest(postingRequestId)
        .orElseThrow(() -> new PostingException("Posting request not found."));
  }

  @Override
  @Transactional
  public PostingResult retry(final UUID postingRequestId) {
    final PostingRequest request = status(postingRequestId);
    if (request.status() == PostingRequest.Status.POSTED) {
      return resultFor(request);
    }
    final AccountingEvent event =
        events
            .findEvent(request.accountingEventId())
            .orElseThrow(() -> new PostingException("Accounting event not found."));
    return execute(request, event);
  }

  private PostingResult execute(final PostingRequest request, final AccountingEvent event) {
    final PostingRule rule = resolve(event);
    final PostingPorts.JournalReference journal = journals.createAndPost(event, rule);
    final PostingRequest posted =
        new PostingRequest(
            request.postingRequestId(),
            request.accountingEventId(),
            PostingRequest.Status.POSTED,
            rule.postingRuleId(),
            rule.version(),
            journal.journalEntryId(),
            null,
            null,
            request.attempts() + 1,
            request.createdAt(),
            Instant.now(clock),
            request.version() + 1);
    events.updateRequest(posted);
    audit.record(event.submittedBy(), "POSTING_EXECUTED", posted.postingRequestId());
    outbox.publish("FinancePostingCompleted", posted.postingRequestId());
    return new PostingResult(
        posted.postingRequestId(),
        event.eventId(),
        posted.status(),
        journal.journalEntryId(),
        journal.journalNumber(),
        null,
        null);
  }

  private PostingRule resolve(final AccountingEvent event) {
    final List<PostingRule> matches =
        rules.findApplicable(event.eventType(), event.companyId(), event.accountingDate());
    if (matches.isEmpty()) {
      throw new PostingException("No applicable posting rule exists.");
    }
    final int best = matches.stream().mapToInt(PostingRule::priority).max().orElseThrow();
    final List<PostingRule> selected =
        matches.stream().filter(rule -> rule.priority() == best).toList();
    if (selected.size() != 1) {
      throw new PostingException("Ambiguous posting rules.");
    }
    audit.record(event.submittedBy(), "POSTING_RULE_RESOLVED", selected.get(0).postingRuleId());
    return selected.get(0);
  }

  private void validateEvent(final AccountingEvent event) {
    companies.requireCompany(event.companyId());
    branches.requireBranch(event.companyId(), event.branchId());
    currencies.requireCurrency(event.currencyCode());
    rates.requireRate(event.currencyCode(), event.exchangeRate(), event.accountingDate());
    periods.requireOpenPeriod(event.companyId(), event.accountingDate());
    dimensions.requireDimensions(event.companyId(), event.dimensions());
  }

  private PostingResult resultFor(final PostingRequest request) {
    return new PostingResult(
        request.postingRequestId(),
        request.accountingEventId(),
        request.status(),
        request.journalEntryId(),
        null,
        request.failureCode(),
        request.failureMessage());
  }
}
