package com.newland.erp.finance.posting.infrastructure;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.newland.erp.finance.posting.application.PostingPorts;
import com.newland.erp.finance.posting.domain.AccountingEvent;
import com.newland.erp.finance.posting.domain.PostingException;
import com.newland.erp.finance.posting.domain.PostingRequest;
import com.newland.erp.finance.posting.domain.PostingRule;
import com.newland.erp.finance.posting.domain.PostingRuleLine;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Primary
@Repository
public final class JooqPostingRepository
    implements PostingPorts.EventRepository, PostingPorts.RuleRepository {
  private static final TypeReference<Map<String, String>> STRING_MAP = new TypeReference<>() {};
  private final DSLContext dsl;
  private final ObjectMapper json;

  public JooqPostingRepository(final DSLContext dslContext, final ObjectMapper objectMapper) {
    dsl = dslContext;
    json = objectMapper;
  }

  public boolean eventIdExists(final UUID id) {
    return dsl.fetchExists(
        DSL.table("finance_accounting_event"), DSL.field("event_id", UUID.class).eq(id));
  }

  public Optional<AccountingEvent> findEvent(final UUID id) {
    return eventWhere(DSL.field("event_id", UUID.class).eq(id));
  }

  public Optional<AccountingEvent> findByIdempotencyKey(final String key) {
    return eventWhere(DSL.field("idempotency_key", String.class).eq(key));
  }

  public AccountingEvent saveEvent(final AccountingEvent e) {
    dsl.insertInto(DSL.table("finance_accounting_event"))
        .columns(
            DSL.field("event_id"), DSL.field("idempotency_key"), DSL.field("event_type"),
            DSL.field("source_module"), DSL.field("source_document_type"),
            DSL.field("source_document_id"), DSL.field("source_document_number"),
            DSL.field("company_id"), DSL.field("branch_id"), DSL.field("event_date"),
            DSL.field("accounting_date"), DSL.field("currency_code"), DSL.field("exchange_rate"),
            DSL.field("amount"), DSL.field("tax_amount"), DSL.field("net_amount"),
            DSL.field("description"), DSL.field("dimensions"), DSL.field("attributes"),
            DSL.field("occurred_at"), DSL.field("submitted_by"), DSL.field("version"))
        .values(
            e.eventId(), e.idempotencyKey(), e.eventType(), e.sourceModule(), e.sourceDocumentType(),
            e.sourceDocumentId(), e.sourceDocumentNumber(), e.companyId(), e.branchId(), e.eventDate(),
            e.accountingDate(), e.currencyCode(), e.exchangeRate(), e.amount(), e.taxAmount(), e.netAmount(),
            e.description(), json(e.dimensions()), json(e.attributes()),
            OffsetDateTime.ofInstant(e.occurredAt(), ZoneOffset.UTC), e.submittedBy(), e.version())
        .execute();
    return e;
  }

  public PostingRequest saveRequest(final PostingRequest request) {
    dsl.insertInto(DSL.table("finance_posting_request"))
        .columns(
            DSL.field("posting_request_id"), DSL.field("accounting_event_id"), DSL.field("status"),
            DSL.field("resolved_posting_rule_id"), DSL.field("resolved_posting_rule_version"),
            DSL.field("journal_entry_id"), DSL.field("failure_code"), DSL.field("failure_message"),
            DSL.field("attempts"), DSL.field("created_at"), DSL.field("updated_at"), DSL.field("version"))
        .values(
            request.postingRequestId(), request.accountingEventId(), request.status().name(),
            request.resolvedPostingRuleId(), request.resolvedPostingRuleVersion(), request.journalEntryId(),
            request.failureCode(), request.failureMessage(), request.attempts(),
            OffsetDateTime.ofInstant(request.createdAt(), ZoneOffset.UTC),
            OffsetDateTime.ofInstant(request.updatedAt(), ZoneOffset.UTC), request.version())
        .execute();
    return request;
  }

  public Optional<PostingRequest> findRequest(final UUID id) {
    return requestWhere(DSL.field("posting_request_id", UUID.class).eq(id));
  }

  public Optional<PostingRequest> findRequestByEvent(final UUID id) {
    return requestWhere(DSL.field("accounting_event_id", UUID.class).eq(id));
  }

  public PostingRequest updateRequest(final PostingRequest request) {
    final int updated =
        dsl.update(DSL.table("finance_posting_request"))
            .set(DSL.field("status", String.class), request.status().name())
            .set(DSL.field("resolved_posting_rule_id", UUID.class), request.resolvedPostingRuleId())
            .set(DSL.field("resolved_posting_rule_version", Integer.class), request.resolvedPostingRuleVersion())
            .set(DSL.field("journal_entry_id", UUID.class), request.journalEntryId())
            .set(DSL.field("failure_code", String.class), request.failureCode())
            .set(DSL.field("failure_message", String.class), request.failureMessage())
            .set(DSL.field("attempts", Integer.class), request.attempts())
            .set(
                DSL.field("updated_at", OffsetDateTime.class),
                OffsetDateTime.ofInstant(request.updatedAt(), ZoneOffset.UTC))
            .set(DSL.field("version", Integer.class), request.version())
            .where(DSL.field("posting_request_id", UUID.class).eq(request.postingRequestId()))
            .and(DSL.field("version", Integer.class).eq(request.version() - 1))
            .and(DSL.field("status", String.class).ne(PostingRequest.Status.POSTED.name()))
            .execute();
    if (updated != 1) {
      throw new PostingException("Posting request was modified concurrently or is terminal.");
    }
    return request;
  }

  public Optional<PostingRequest> claimRequest(final UUID requestId, final int expectedVersion) {
    final int claimed =
        dsl.update(DSL.table("finance_posting_request"))
            .set(DSL.field("status", String.class), PostingRequest.Status.VALIDATING.name())
            .setNull(DSL.field("failure_code", String.class))
            .setNull(DSL.field("failure_message", String.class))
            .set(DSL.field("attempts", Integer.class), DSL.field("attempts", Integer.class).plus(1))
            .set(DSL.field("version", Integer.class), expectedVersion + 1)
            .set(DSL.field("updated_at", OffsetDateTime.class), OffsetDateTime.now(ZoneOffset.UTC))
            .where(DSL.field("posting_request_id", UUID.class).eq(requestId))
            .and(DSL.field("version", Integer.class).eq(expectedVersion))
            .and(
                DSL.field("status", String.class)
                    .in(
                        PostingRequest.Status.RECEIVED.name(),
                        PostingRequest.Status.FAILED.name(),
                        PostingRequest.Status.VALIDATING.name()))
            .execute();
    return claimed == 1 ? findRequest(requestId) : Optional.empty();
  }

  public void lockEvent(final UUID eventId) {
    dsl.fetch("select pg_advisory_xact_lock(?)", eventId.getMostSignificantBits() ^ eventId.getLeastSignificantBits());
  }

  public List<PostingRule> findApplicable(
      final String eventType, final UUID companyId, final LocalDate date) {
    final List<PostingRule> matches =
        dsl.selectFrom(DSL.table("finance_posting_rule"))
        .where(DSL.field("event_type", String.class).eq(eventType))
        .and(DSL.field("status", String.class).eq(PostingRule.Status.ACTIVE.name()))
        .and(
            DSL.field("company_id", UUID.class)
                .isNull()
                .or(DSL.field("company_id", UUID.class).eq(companyId)))
        .and(DSL.field("effective_from", LocalDate.class).le(date))
        .and(
            DSL.field("effective_to", LocalDate.class)
                .isNull()
                .or(DSL.field("effective_to", LocalDate.class).ge(date)))
        .orderBy(DSL.field("company_id").isNotNull().desc(), DSL.field("priority").desc())
        .fetch(this::rule);
    final boolean hasCompanyRule =
        matches.stream().anyMatch(rule -> companyId.equals(rule.companyId()));
    return hasCompanyRule
        ? matches.stream().filter(rule -> companyId.equals(rule.companyId())).toList()
        : matches.stream().filter(rule -> rule.companyId() == null).toList();
  }

  public PostingRule save(final PostingRule rule) {
    dsl.insertInto(DSL.table("finance_posting_rule"))
        .columns(
            DSL.field("posting_rule_id"), DSL.field("code"), DSL.field("name"), DSL.field("event_type"),
            DSL.field("company_id"), DSL.field("effective_from"), DSL.field("effective_to"),
            DSL.field("priority"), DSL.field("status"), DSL.field("version"),
            DSL.field("created_at"), DSL.field("created_by"), DSL.field("updated_at"), DSL.field("updated_by"))
        .values(
            rule.postingRuleId(), rule.code(), rule.name(), rule.eventType(), rule.companyId(),
            rule.effectiveFrom(), rule.effectiveTo(), rule.priority(), rule.status().name(), rule.version(),
            OffsetDateTime.ofInstant(rule.createdAt(), ZoneOffset.UTC), rule.createdBy(),
            rule.updatedAt() == null
                ? null
                : OffsetDateTime.ofInstant(rule.updatedAt(), ZoneOffset.UTC),
            rule.updatedBy())
        .execute();
    for (PostingRuleLine line : rule.lines()) {
      dsl.insertInto(DSL.table("finance_posting_rule_line"))
          .columns(
              DSL.field("posting_rule_line_id"), DSL.field("posting_rule_id"), DSL.field("line_number"),
              DSL.field("direction"), DSL.field("account_resolution_type"), DSL.field("fixed_account_id"),
              DSL.field("account_attribute_key"), DSL.field("amount_expression"), DSL.field("constant_amount"),
              DSL.field("description_template"), DSL.field("dimension_mappings"))
          .values(
              line.id(), rule.postingRuleId(), line.lineNumber(), line.direction().name(),
              line.accountResolutionType().name(), line.fixedAccountId(), line.accountAttributeKey(),
              line.amountExpression().name(),
              line.constantAmount(),
              line.descriptionTemplate(),
              json(line.dimensionMappings()))
          .execute();
    }
    return rule;
  }

  public PostingRule transition(
      final PostingRule rule, final PostingRule.Status expectedStatus) {
    final int updated =
        dsl.update(DSL.table("finance_posting_rule"))
            .set(DSL.field("status", String.class), rule.status().name())
            .set(
                DSL.field("updated_at", OffsetDateTime.class),
                OffsetDateTime.ofInstant(rule.updatedAt(), ZoneOffset.UTC))
            .set(DSL.field("updated_by", String.class), rule.updatedBy())
            .where(DSL.field("posting_rule_id", UUID.class).eq(rule.postingRuleId()))
            .and(DSL.field("status", String.class).eq(expectedStatus.name()))
            .execute();
    if (updated != 1) {
      throw new PostingException("Posting rule was modified concurrently.");
    }
    return rule;
  }

  public Optional<PostingRule> findRule(final UUID postingRuleId) {
    return dsl.selectFrom(DSL.table("finance_posting_rule"))
        .where(DSL.field("posting_rule_id", UUID.class).eq(postingRuleId))
        .fetchOptional(this::rule);
  }

  public Optional<PostingRule> findLatest(final String code, final UUID companyId) {
    return dsl.selectFrom(DSL.table("finance_posting_rule"))
        .where(DSL.field("code", String.class).eq(code))
        .and(scope(companyId))
        .orderBy(DSL.field("version", Integer.class).desc())
        .limit(1)
        .fetchOptional(this::rule);
  }

  public void lockActivationScope(final PostingRule candidate) {
    final String scopeKey =
        "posting-rule:"
            + (candidate.companyId() == null ? "GLOBAL" : candidate.companyId());
    dsl.fetch("select pg_advisory_xact_lock(hashtextextended(?, 0))", scopeKey);
  }

  public List<PostingRule> findActivationConflicts(final PostingRule candidate) {
    org.jooq.SelectConditionStep<Record> query =
        dsl.selectFrom(DSL.table("finance_posting_rule"))
            .where(DSL.field("posting_rule_id", UUID.class).ne(candidate.postingRuleId()))
            .and(DSL.field("event_type", String.class).eq(candidate.eventType()))
            .and(scope(candidate.companyId()))
            .and(DSL.field("status", String.class).eq(PostingRule.Status.ACTIVE.name()))
            .and(
                DSL.field("code", String.class)
                    .eq(candidate.code())
                    .or(DSL.field("priority", Integer.class).eq(candidate.priority())))
            .and(
                DSL.field("effective_to", LocalDate.class)
                    .isNull()
                    .or(DSL.field("effective_to", LocalDate.class).ge(candidate.effectiveFrom())));
    if (candidate.effectiveTo() != null) {
      query =
          query.and(
              DSL.field("effective_from", LocalDate.class).le(candidate.effectiveTo()));
    }
    return query.fetch(this::rule);
  }

  public List<PostingRule> list() {
    return dsl.selectFrom(DSL.table("finance_posting_rule"))
        .orderBy(DSL.field("code"), DSL.field("version", Integer.class).desc())
        .fetch(this::rule);
  }

  public List<PostingRule> list(final UUID companyId) {
    return dsl.selectFrom(DSL.table("finance_posting_rule"))
        .where(scope(companyId))
        .orderBy(DSL.field("code"), DSL.field("version", Integer.class).desc())
        .fetch(this::rule);
  }

  private Optional<AccountingEvent> eventWhere(final org.jooq.Condition condition) {
    return dsl.selectFrom(DSL.table("finance_accounting_event")).where(condition).fetchOptional(this::event);
  }

  private Optional<PostingRequest> requestWhere(final org.jooq.Condition condition) {
    return dsl.selectFrom(DSL.table("finance_posting_request")).where(condition).fetchOptional(this::request);
  }

  private AccountingEvent event(final Record r) {
    return new AccountingEvent(
        r.get("event_id", UUID.class), r.get("idempotency_key", String.class), r.get("event_type", String.class),
        r.get("source_module", String.class), r.get("source_document_type", String.class),
        r.get("source_document_id", UUID.class), r.get("source_document_number", String.class),
        r.get("company_id", UUID.class), r.get("branch_id", UUID.class), r.get("event_date", LocalDate.class),
        r.get("accounting_date", LocalDate.class), r.get("currency_code", String.class),
        r.get("exchange_rate", BigDecimal.class), r.get("amount", BigDecimal.class),
        r.get("tax_amount", BigDecimal.class),
        r.get("net_amount", BigDecimal.class),
        r.get("description", String.class),
        map(r.get("dimensions", JSONB.class)), map(r.get("attributes", JSONB.class)),
        r.get("occurred_at", OffsetDateTime.class).toInstant(),
        r.get("submitted_by", String.class),
        r.get("version", Integer.class));
  }

  private PostingRequest request(final Record r) {
    return new PostingRequest(
        r.get("posting_request_id", UUID.class), r.get("accounting_event_id", UUID.class),
        PostingRequest.Status.valueOf(r.get("status", String.class)), r.get("resolved_posting_rule_id", UUID.class),
        r.get("resolved_posting_rule_version", Integer.class), r.get("journal_entry_id", UUID.class),
        r.get("failure_code", String.class), r.get("failure_message", String.class), r.get("attempts", Integer.class),
        r.get("created_at", OffsetDateTime.class).toInstant(), r.get("updated_at", OffsetDateTime.class).toInstant(),
        r.get("version", Integer.class));
  }

  private PostingRule rule(final Record r) {
    final UUID id = r.get("posting_rule_id", UUID.class);
    final List<PostingRuleLine> lines =
        dsl.selectFrom(DSL.table("finance_posting_rule_line"))
            .where(DSL.field("posting_rule_id", UUID.class).eq(id))
            .orderBy(DSL.field("line_number"))
            .fetch(this::line);
    final OffsetDateTime updated = r.get("updated_at", OffsetDateTime.class);
    return new PostingRule(
        id, r.get("code", String.class), r.get("name", String.class), r.get("event_type", String.class),
        r.get("company_id", UUID.class),
        r.get("effective_from", LocalDate.class),
        r.get("effective_to", LocalDate.class),
        r.get("priority", Integer.class), PostingRule.Status.valueOf(r.get("status", String.class)),
        r.get("version", Integer.class), lines, r.get("created_at", OffsetDateTime.class).toInstant(),
        r.get("created_by", String.class),
        updated == null ? null : updated.toInstant(),
        r.get("updated_by", String.class));
  }

  private PostingRuleLine line(final Record r) {
    return new PostingRuleLine(
        r.get("posting_rule_line_id", UUID.class), r.get("line_number", Integer.class),
        PostingRuleLine.Direction.valueOf(r.get("direction", String.class)),
        PostingRuleLine.AccountResolutionType.valueOf(r.get("account_resolution_type", String.class)),
        r.get("fixed_account_id", UUID.class), r.get("account_attribute_key", String.class),
        PostingRuleLine.AmountExpression.valueOf(r.get("amount_expression", String.class)),
        r.get("constant_amount", BigDecimal.class), r.get("description_template", String.class),
        map(r.get("dimension_mappings", JSONB.class)));
  }

  private JSONB json(final Map<String, String> value) {
    try {
      return JSONB.valueOf(json.writeValueAsString(value));
    } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
      throw new PostingException("Posting JSON serialization failed.");
    }
  }

  private Map<String, String> map(final JSONB value) {
    try {
      return json.readValue(value.data(), STRING_MAP);
    } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
      throw new PostingException("Posting JSON deserialization failed.");
    }
  }

  private Condition scope(final UUID companyId) {
    return companyId == null
        ? DSL.field("company_id", UUID.class).isNull()
        : DSL.field("company_id", UUID.class).eq(companyId);
  }
}
