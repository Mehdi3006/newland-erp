package com.newland.erp.procurement.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.newland.erp.procurement.application.ProcurementAccountingPublicationRepository;
import com.newland.erp.procurement.application.ProcurementAccountingService;
import com.newland.erp.procurement.domain.ProcurementAccountingEvent;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

@Repository
public final class JooqProcurementAccountingPublicationRepository
    implements ProcurementAccountingPublicationRepository {
  private static final TypeReference<Map<String, String>> STRING_MAP = new TypeReference<>() {};
  private final DSLContext dsl;
  private final ObjectMapper json;

  public JooqProcurementAccountingPublicationRepository(
      final DSLContext dslContext, final ObjectMapper objectMapper) {
    dsl = dslContext;
    json = objectMapper;
  }

  @Override
  public boolean insertIfAbsent(final ProcurementAccountingEvent event) {
    return dsl.insertInto(DSL.table("procurement_accounting_publication"))
            .columns(
                field("event_id"), field("idempotency_key"), field("event_type"),
                field("reference_document_type"), field("reference_document_id"),
                field("reference_document_number"), field("supplier_id"), field("company_id"),
                field("branch_id"), field("event_date"), field("accounting_date"),
                field("currency_code"), field("exchange_rate"), field("amount"),
                field("tax_amount"), field("net_amount"), field("cost_center_id"),
                field("profit_center_id"), field("financial_dimensions"), field("description"),
                field("occurred_at"), field("actor"), field("status"))
            .values(
                event.eventId(), event.idempotencyKey(), event.eventType().name(),
                event.referenceDocumentType(), event.referenceDocumentId(),
                event.referenceDocumentNumber(), event.supplierId(), event.companyId(),
                event.branchId(), event.eventDate(), event.accountingDate(), event.currencyCode(),
                event.exchangeRate(), event.amount(), event.taxAmount(), event.netAmount(),
                event.costCenterId(), event.profitCenterId(), json(event.financialDimensions()),
                event.description(), event.occurredAt(), event.actor(), "PENDING")
            .onConflictDoNothing()
            .execute()
        == 1;
  }

  @Override
  public Optional<Publication> findByEventId(final UUID eventId) {
    return find(DSL.field("event_id", UUID.class).eq(eventId));
  }

  @Override
  public Optional<Publication> findByIdempotencyKey(final String idempotencyKey) {
    return find(DSL.field("idempotency_key", String.class).eq(idempotencyKey));
  }

  @Override
  public void complete(
      final UUID eventId, final ProcurementAccountingService.PostingReceipt receipt) {
    final int updated =
        dsl.update(DSL.table("procurement_accounting_publication"))
            .set(DSL.field("status", String.class), receipt.status())
            .set(DSL.field("posting_request_id", UUID.class), receipt.postingRequestId())
            .set(DSL.field("journal_entry_id", UUID.class), receipt.journalEntryId())
            .set(DSL.field("journal_number", String.class), receipt.journalNumber())
            .set(DSL.field("failure_code", String.class), receipt.failureCode())
            .set(DSL.field("failure_message", String.class), receipt.failureMessage())
            .set(DSL.field("updated_at", OffsetDateTime.class), OffsetDateTime.now())
            .where(DSL.field("event_id", UUID.class).eq(eventId))
            .and(DSL.field("status", String.class).eq("PENDING"))
            .execute();
    if (updated != 1) {
      throw new IllegalStateException("Procurement accounting publication is already terminal.");
    }
  }

  private Optional<Publication> find(final org.jooq.Condition condition) {
    return dsl.selectFrom(DSL.table("procurement_accounting_publication"))
        .where(condition)
        .fetchOptional(this::publication);
  }

  private Publication publication(final Record row) {
    final ProcurementAccountingEvent event =
        new ProcurementAccountingEvent(
            row.get("event_id", UUID.class),
            row.get("idempotency_key", String.class),
            ProcurementAccountingEvent.EventType.valueOf(row.get("event_type", String.class)),
            row.get("reference_document_type", String.class),
            row.get("reference_document_id", UUID.class),
            row.get("reference_document_number", String.class),
            row.get("supplier_id", UUID.class),
            row.get("company_id", UUID.class),
            row.get("branch_id", UUID.class),
            row.get("event_date", java.time.LocalDate.class),
            row.get("accounting_date", java.time.LocalDate.class),
            row.get("currency_code", String.class),
            row.get("exchange_rate", BigDecimal.class),
            row.get("amount", BigDecimal.class),
            row.get("tax_amount", BigDecimal.class),
            row.get("net_amount", BigDecimal.class),
            row.get("cost_center_id", UUID.class),
            row.get("profit_center_id", UUID.class),
            dimensions(row.get("financial_dimensions", JSONB.class)),
            row.get("description", String.class),
            row.get("occurred_at", OffsetDateTime.class).toInstant(),
            row.get("actor", String.class));
    return new Publication(
        event,
        row.get("status", String.class),
        row.get("posting_request_id", UUID.class),
        row.get("journal_entry_id", UUID.class),
        row.get("journal_number", String.class),
        row.get("failure_code", String.class),
        row.get("failure_message", String.class));
  }

  private JSONB json(final Map<String, String> value) {
    try {
      return JSONB.valueOf(json.writeValueAsString(value));
    } catch (JsonProcessingException exception) {
      throw new IllegalArgumentException("Cannot serialize Procurement dimensions.", exception);
    }
  }

  private Map<String, String> dimensions(final JSONB value) {
    try {
      return json.readValue(value.data(), STRING_MAP);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Cannot read Procurement dimensions.", exception);
    }
  }

  private static org.jooq.Field<Object> field(final String name) {
    return DSL.field(name);
  }
}
