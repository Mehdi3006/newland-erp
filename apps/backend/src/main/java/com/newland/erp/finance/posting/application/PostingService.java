package com.newland.erp.finance.posting.application;

import com.newland.erp.finance.posting.domain.AccountingEvent;
import com.newland.erp.finance.posting.domain.PostingException;
import com.newland.erp.finance.posting.domain.PostingRequest;
import com.newland.erp.finance.posting.domain.PostingResult;
import com.newland.erp.finance.posting.domain.PostingRule;
import com.newland.erp.finance.domain.FinanceException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

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
  private final PostingPorts.AuthorizationPort authorization;
  private final PostingPorts.CurrentUserPort currentUsers;
  private final Clock clock;
  private final TransactionTemplate transactions;

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
      final PostingPorts.AuthorizationPort authorizationPort,
      final PostingPorts.CurrentUserPort currentUserPort,
      final PlatformTransactionManager transactionManager,
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
    authorization = authorizationPort;
    currentUsers = currentUserPort;
    clock = systemClock;
    transactions = new TransactionTemplate(transactionManager);
  }

  @Override
  public PostingResult submit(final AccountingEvent event) {
    authorization.require(event.submittedBy(), "finance.posting.submit", event.companyId());
    final AcceptedPosting accepted = accept(event);
    final PostingRequest request = accepted.request();
    if (request.status() == PostingRequest.Status.REJECTED
        || request.status() == PostingRequest.Status.POSTED) {
      return resultFor(request);
    }
    return execute(request, accepted.event(), false);
  }

  @Override
  public PostingResult preview(final AccountingEvent event) {
    authorization.require(event.submittedBy(), "finance.posting.preview", event.companyId());
    validateEvent(event);
    final PostingRule rule = resolve(event);
    audit.record(event.submittedBy(), "POSTING_PREVIEWED", event.eventId());
    return new PostingResult(
        null, event.eventId(), PostingRequest.Status.RULE_RESOLVED, null, null, null, rule.code());
  }

  @Override
  public PostingRequest status(final UUID postingRequestId) {
    final PostingRequest request = events
        .findRequest(postingRequestId)
        .orElseThrow(() -> new PostingException("Posting request not found."));
    final AccountingEvent event =
        events
            .findEvent(request.accountingEventId())
            .orElseThrow(() -> new PostingException("Accounting event not found."));
    authorization.require(currentUsers.currentUser(), "finance.posting.status", event.companyId());
    return request;
  }

  @Override
  public PostingResult retry(final UUID postingRequestId) {
    final PostingRequest request = status(postingRequestId);
    if (request.status() == PostingRequest.Status.POSTED
        || request.status() == PostingRequest.Status.REJECTED) {
      return resultFor(request);
    }
    final AccountingEvent event =
        events
            .findEvent(request.accountingEventId())
            .orElseThrow(() -> new PostingException("Accounting event not found."));
    authorization.require(currentUsers.currentUser(), "finance.posting.retry", event.companyId());
    return execute(request, event, true);
  }

  private PostingResult execute(
      final PostingRequest request, final AccountingEvent event, final boolean retry) {
    try {
      return transactions.execute(
          status -> {
            events.lockEvent(event.eventId());
            final PostingRequest current =
                events
                    .findRequest(request.postingRequestId())
                    .orElseThrow(() -> new PostingException("Posting request not found."));
            if (current.status() == PostingRequest.Status.POSTED
                || current.status() == PostingRequest.Status.REJECTED) {
              return resultFor(current);
            }
            final PostingRequest claimed =
                events
                    .claimRequest(current.postingRequestId(), current.version())
                    .orElseThrow(() -> new PostingException("Posting request claim failed."));
            if (retry) {
              audit.record(event.submittedBy(), "POSTING_RETRIED", claimed.postingRequestId());
            }
            final PostingRule rule = resolve(event);
            final PostingRequest resolved =
                transition(
                    claimed,
                    PostingRequest.Status.RULE_RESOLVED,
                    rule.postingRuleId(),
                    rule.version(),
                    null,
                    null,
                    null);
            final PostingPorts.JournalReference journal = journals.createAndPost(event, rule);
            final PostingRequest journalCreated =
                transition(
                    resolved,
                    PostingRequest.Status.JOURNAL_CREATED,
                    rule.postingRuleId(),
                    rule.version(),
                    journal.journalEntryId(),
                    null,
                    null);
            final PostingRequest posted =
                transition(
                    journalCreated,
                    PostingRequest.Status.POSTED,
                    rule.postingRuleId(),
                    rule.version(),
                    journal.journalEntryId(),
                    null,
                    null);
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
          });
    } catch (RuntimeException exception) {
      return persistFailure(request.postingRequestId(), event, exception);
    }
  }

  private AcceptedPosting accept(final AccountingEvent event) {
    try {
      return transactions.execute(
          status -> {
            events.lockEvent(event.eventId());
            final var existing = events.findByIdempotencyKey(event.idempotencyKey());
            if (existing.isPresent()) {
              final AccountingEvent persisted = requireMatchingPayload(existing.get(), event);
              final PostingRequest request =
                  events.findRequestByEvent(persisted.eventId()).orElseThrow();
              return new AcceptedPosting(request, persisted);
            }
            if (events.eventIdExists(event.eventId())) {
              throw new PostingException("Duplicate accounting event ID.");
            }
            events.saveEvent(event);
            final Instant now = Instant.now(clock);
            final PostingRequest received =
                events.saveRequest(
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
                        now,
                        now,
                        0));
            audit.record(event.submittedBy(), "ACCOUNTING_EVENT_RECEIVED", event.eventId());
            outbox.publish("FinanceAccountingEventAccepted", event.eventId());
            try {
              validateEvent(event);
              return new AcceptedPosting(received, event);
            } catch (RuntimeException exception) {
              return new AcceptedPosting(reject(received, exception), event);
            }
          });
    } catch (DataIntegrityViolationException exception) {
      final AccountingEvent existing =
          events
              .findByIdempotencyKey(event.idempotencyKey())
              .orElseThrow(() -> exception);
      final AccountingEvent persisted = requireMatchingPayload(existing, event);
      final PostingRequest request =
          events.findRequestByEvent(persisted.eventId()).orElseThrow(() -> exception);
      return new AcceptedPosting(request, persisted);
    }
  }

  private AccountingEvent requireMatchingPayload(
      final AccountingEvent persisted, final AccountingEvent candidate) {
    if (!persisted.hasSameIdempotencyPayload(candidate)) {
      throw new PostingException(
          "Idempotency key was reused with conflicting accounting event data.");
    }
    return persisted;
  }

  private PostingRequest reject(final PostingRequest request, final RuntimeException exception) {
    final PostingRequest rejected =
        failureTransition(
            request,
            PostingRequest.Status.REJECTED,
            null,
            null,
            null,
            failureCode(exception),
            safeMessage(exception));
    audit.record(
        currentUsers.currentUser(), "POSTING_VALIDATION_FAILED", rejected.postingRequestId());
    outbox.publish("FinancePostingRejected", rejected.postingRequestId());
    return rejected;
  }

  private PostingResult persistFailure(
      final UUID postingRequestId,
      final AccountingEvent event,
      final RuntimeException exception) {
    return transactions.execute(
        status -> {
          events.lockEvent(event.eventId());
          final PostingRequest current =
              events
                  .findRequest(postingRequestId)
                  .orElseThrow(() -> new PostingException("Posting request not found."));
          if (current.status() == PostingRequest.Status.POSTED) {
            return resultFor(current);
          }
          final PostingRequest.Status failureStatus =
              isDeterministic(exception)
                  ? PostingRequest.Status.REJECTED
                  : PostingRequest.Status.FAILED;
          final PostingRequest failed =
              failureTransition(
                  current,
                  failureStatus,
                  current.resolvedPostingRuleId(),
                  current.resolvedPostingRuleVersion(),
                  current.journalEntryId(),
                  failureCode(exception),
                  safeMessage(exception));
          final boolean rejected = failureStatus == PostingRequest.Status.REJECTED;
          audit.record(
              event.submittedBy(),
              rejected ? "POSTING_VALIDATION_FAILED" : "POSTING_FAILED",
              postingRequestId);
          outbox.publish(
              rejected ? "FinancePostingRejected" : "FinancePostingFailed", postingRequestId);
          return resultFor(failed);
        });
  }

  private PostingRequest transition(
      final PostingRequest current,
      final PostingRequest.Status status,
      final UUID ruleId,
      final Integer ruleVersion,
      final UUID journalId,
      final String failureCode,
      final String failureMessage) {
    return events.updateRequest(
        new PostingRequest(
            current.postingRequestId(),
            current.accountingEventId(),
            status,
            ruleId,
            ruleVersion,
            journalId,
            failureCode,
            failureMessage,
            current.attempts(),
            current.createdAt(),
            Instant.now(clock),
            current.version() + 1));
  }

  private PostingRequest failureTransition(
      final PostingRequest current,
      final PostingRequest.Status status,
      final UUID ruleId,
      final Integer ruleVersion,
      final UUID journalId,
      final String failureCode,
      final String failureMessage) {
    return events.updateRequest(
        new PostingRequest(
            current.postingRequestId(),
            current.accountingEventId(),
            status,
            ruleId,
            ruleVersion,
            journalId,
            failureCode,
            failureMessage,
            current.attempts() + 1,
            current.createdAt(),
            Instant.now(clock),
            current.version() + 1));
  }

  private boolean isDeterministic(final RuntimeException exception) {
    return exception instanceof PostingException
        || exception instanceof FinanceException
        || exception instanceof IllegalArgumentException;
  }

  private String failureCode(final RuntimeException exception) {
    if (exception instanceof FinanceException) {
      return "FINANCE_VALIDATION_FAILED";
    }
    if (exception instanceof PostingException || exception instanceof IllegalArgumentException) {
      return "POSTING_VALIDATION_FAILED";
    }
    return "POSTING_TECHNICAL_FAILURE";
  }

  private String safeMessage(final RuntimeException exception) {
    if (!isDeterministic(exception)) {
      return "Posting failed due to a temporary technical error.";
    }
    final String message = exception.getMessage();
    return message == null || message.isBlank()
        ? "Posting validation failed."
        : message.substring(0, Math.min(message.length(), 1000));
  }

  private PostingRule resolve(final AccountingEvent event) {
    final List<PostingRule> matches =
        rules.findApplicable(event.eventType(), event.companyId(), event.accountingDate());
    if (matches.isEmpty()) {
      throw new PostingException("No applicable posting rule exists.");
    }
    final boolean companySpecific =
        matches.stream().anyMatch(rule -> event.companyId().equals(rule.companyId()));
    final List<PostingRule> scoped =
        companySpecific
            ? matches.stream()
                .filter(rule -> event.companyId().equals(rule.companyId()))
                .toList()
            : matches.stream().filter(rule -> rule.companyId() == null).toList();
    final int best = scoped.stream().mapToInt(PostingRule::priority).max().orElseThrow();
    final List<PostingRule> selected =
        scoped.stream().filter(rule -> rule.priority() == best).toList();
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
    rates.requireRate(event.companyId(), event.currencyCode(), event.exchangeRate(),
        event.accountingDate());
    periods.requireOpenPeriod(event.companyId(), event.accountingDate());
    dimensions.requireDimensions(event.companyId(), event.dimensions());
  }

  private PostingResult resultFor(final PostingRequest request) {
    final PostingPorts.JournalReference reference =
        request.journalEntryId() == null
            ? null
            : journals.findReference(request.journalEntryId());
    return new PostingResult(
        request.postingRequestId(),
        request.accountingEventId(),
        request.status(),
        request.journalEntryId(),
        reference == null ? null : reference.journalNumber(),
        request.failureCode(),
        request.failureMessage());
  }

  private record AcceptedPosting(PostingRequest request, AccountingEvent event) {}
}
