package com.newland.erp.servicewarranty.infrastructure;

import com.newland.erp.servicewarranty.application.ServiceWarrantyRepository;
import com.newland.erp.servicewarranty.domain.ServiceTicket;
import com.newland.erp.servicewarranty.domain.WarrantyPolicy;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;
import org.springframework.dao.DataIntegrityViolationException;

@Repository
public class JooqServiceWarrantyRepository implements ServiceWarrantyRepository {
  private final DSLContext dsl;

  public JooqServiceWarrantyRepository(final DSLContext dslContext) {
    dsl = dslContext;
  }

  @Override
  public boolean insertTicketIfAbsent(final ServiceTicket ticket) {
    return dsl.insertInto(DSL.table("service_ticket"))
            .columns(
                field("id"), field("idempotency_key"), field("ticket_number"),
                field("company_id"), field("branch_id"), field("customer_id"),
                field("product_id"), field("sku_id"), field("serial_code"),
                field("sales_order_id"), field("purchase_date"), field("issue_summary"),
                field("status"), field("version"), field("created_at"), field("updated_at"),
                field("actor"))
            .values(
                ticket.id(), ticket.idempotencyKey(), ticket.ticketNumber(), ticket.companyId(),
                ticket.branchId(), ticket.customerId(), ticket.productId(), ticket.skuId(),
                ticket.serialCode(), ticket.salesOrderId(), ticket.purchaseDate(),
                ticket.issueSummary(), ticket.status().name(), ticket.version(),
                ticket.createdAt(), ticket.updatedAt(), ticket.actor())
            .onConflict(DSL.field("idempotency_key"))
            .doNothing()
            .execute()
        == 1;
  }

  @Override
  public Optional<ServiceTicket> findTicket(final UUID ticketId) {
    return dsl.selectFrom(DSL.table("service_ticket"))
        .where(uuid("id").eq(ticketId))
        .fetchOptional(this::ticket);
  }

  @Override
  public Optional<ServiceTicket> findTicket(final UUID ticketId, final UUID companyId) {
    return dsl.selectFrom(DSL.table("service_ticket"))
        .where(uuid("id").eq(ticketId).and(uuid("company_id").eq(companyId)))
        .fetchOptional(this::ticket);
  }

  @Override
  public Optional<ServiceTicket> findTicketByIdempotencyKey(final String idempotencyKey) {
    return dsl.selectFrom(DSL.table("service_ticket"))
        .where(text("idempotency_key").eq(idempotencyKey))
        .fetchOptional(this::ticket);
  }

  @Override
  public ServiceTicket updateTicket(final ServiceTicket ticket) {
    final int updated =
        dsl.update(DSL.table("service_ticket"))
            .set(text("status"), ticket.status().name())
            .set(longField("version"), ticket.version())
            .set(field("updated_at"), ticket.updatedAt())
            .where(uuid("id").eq(ticket.id()))
            .and(longField("version").eq(ticket.version() - 1))
            .execute();
    if (updated != 1) {
      throw new IllegalStateException("Service ticket was modified concurrently.");
    }
    persistDecision(ticket);
    persistDiagnosis(ticket);
    persistResolution(ticket);
    return ticket;
  }

  @Override
  public WarrantyPolicy insertPolicy(final WarrantyPolicy policy) {
    try {
      dsl.insertInto(DSL.table("service_warranty_policy"))
          .columns(
              field("id"), field("company_id"), field("product_id"), field("duration_days"),
              field("serial_required"), field("sales_evidence_required"), field("effective_from"),
              field("effective_to"), field("active"))
          .values(
              policy.id(), policy.companyId(), policy.productId(), policy.durationDays(),
              policy.serialRequired(), policy.salesEvidenceRequired(), policy.effectiveFrom(),
              policy.effectiveTo(), policy.active())
          .execute();
    } catch (org.jooq.exception.DataAccessException exception) {
      throw new DataIntegrityViolationException("Warranty policy persistence failed.", exception);
    }
    return policy;
  }

  @Override
  public boolean hasOverlappingPolicy(final WarrantyPolicy policy) {
    if (!policy.active()) {
      return false;
    }
    var overlap =
        uuid("company_id")
            .eq(policy.companyId())
            .and(uuid("product_id").isNotDistinctFrom(policy.productId()))
            .and(bool("active").eq(true))
            .and(
                date("effective_to")
                    .isNull()
                    .or(date("effective_to").ge(policy.effectiveFrom())));
    if (policy.effectiveTo() != null) {
      overlap = overlap.and(date("effective_from").le(policy.effectiveTo()));
    }
    return dsl.fetchExists(
        dsl.selectOne()
            .from(DSL.table("service_warranty_policy"))
            .where(overlap));
  }

  @Override
  public Optional<WarrantyPolicy> resolvePolicy(
      final UUID companyId, final UUID productId, final java.time.LocalDate effectiveDate) {
    return dsl.selectFrom(DSL.table("service_warranty_policy"))
        .where(
            uuid("company_id").eq(companyId)
                .and(bool("active").eq(true))
                .and(
                    uuid("product_id").eq(productId)
                        .or(uuid("product_id").isNull()))
                .and(date("effective_from").le(effectiveDate))
                .and(date("effective_to").isNull().or(date("effective_to").ge(effectiveDate))))
        .orderBy(uuid("product_id").desc().nullsLast(), date("effective_from").desc())
        .limit(1)
        .fetchOptional(this::policy);
  }

  private ServiceTicket ticket(final Record row) {
    final UUID id = row.get(uuid("id"));
    return new ServiceTicket(
        id, row.get(text("idempotency_key")), row.get(text("ticket_number")),
        row.get(uuid("company_id")), row.get(uuid("branch_id")), row.get(uuid("customer_id")),
        row.get(uuid("product_id")), row.get(uuid("sku_id")), row.get(text("serial_code")),
        row.get(uuid("sales_order_id")), localDate(row, "purchase_date"),
        row.get(text("issue_summary")), ServiceTicket.Status.valueOf(row.get(text("status"))),
        decision(id), diagnosis(id), resolution(id), row.get(longField("version")),
        instant(row, "created_at"), instant(row, "updated_at"), row.get(text("actor")));
  }

  private WarrantyPolicy policy(final Record row) {
    return new WarrantyPolicy(
        row.get(uuid("id")), row.get(uuid("company_id")), row.get(uuid("product_id")),
        row.get(integer("duration_days")), Boolean.TRUE.equals(row.get(bool("serial_required"))),
        Boolean.TRUE.equals(row.get(bool("sales_evidence_required"))),
        localDate(row, "effective_from"), localDate(row, "effective_to"),
        Boolean.TRUE.equals(row.get(bool("active"))));
  }

  private void persistDecision(final ServiceTicket ticket) {
    final var value = ticket.warrantyDecision();
    if (value == null) {
      return;
    }
    dsl.insertInto(DSL.table("service_warranty_decision"))
        .columns(
            field("id"), field("ticket_id"), field("policy_id"), field("eligible"),
            field("reason"), field("coverage_ends_on"), field("decided_at"), field("actor"))
        .values(
            value.id(), ticket.id(), value.policyId(), value.eligible(), value.reason(),
            value.coverageEndsOn(), value.decidedAt(), value.actor())
        .onConflict(DSL.field("ticket_id"))
        .doNothing()
        .execute();
  }

  private void persistDiagnosis(final ServiceTicket ticket) {
    final var value = ticket.diagnosis();
    if (value == null) {
      return;
    }
    dsl.insertInto(DSL.table("service_diagnosis"))
        .columns(
            field("id"), field("ticket_id"), field("findings"), field("recommendation"),
            field("diagnosed_at"))
        .values(
            value.id(), ticket.id(), value.findings(), value.recommendation(), value.diagnosedAt())
        .onConflict(DSL.field("ticket_id"))
        .doNothing()
        .execute();
  }

  private void persistResolution(final ServiceTicket ticket) {
    final var value = ticket.resolution();
    if (value == null) {
      return;
    }
    dsl.insertInto(DSL.table("service_resolution"))
        .columns(
            field("id"), field("ticket_id"), field("resolution_type"), field("outcome"),
            field("resolved_at"))
        .values(
            value.id(), ticket.id(), value.type().name(), value.outcome(), value.resolvedAt())
        .onConflict(DSL.field("ticket_id"))
        .doUpdate()
        .set(text("outcome"), value.outcome())
        .set(field("resolved_at"), value.resolvedAt())
        .execute();
  }

  private ServiceTicket.WarrantyDecision decision(final UUID ticketId) {
    return dsl.selectFrom(DSL.table("service_warranty_decision"))
        .where(uuid("ticket_id").eq(ticketId))
        .fetchOne(
            row ->
                new ServiceTicket.WarrantyDecision(
                    row.get(uuid("id")), row.get(uuid("policy_id")),
                    Boolean.TRUE.equals(row.get(bool("eligible"))), row.get(text("reason")),
                    localDate(row, "coverage_ends_on"), instant(row, "decided_at"),
                    row.get(text("actor"))));
  }

  private ServiceTicket.Diagnosis diagnosis(final UUID ticketId) {
    return dsl.selectFrom(DSL.table("service_diagnosis"))
        .where(uuid("ticket_id").eq(ticketId))
        .fetchOne(
            row ->
                new ServiceTicket.Diagnosis(
                    row.get(uuid("id")), row.get(text("findings")),
                    row.get(text("recommendation")), instant(row, "diagnosed_at")));
  }

  private ServiceTicket.Resolution resolution(final UUID ticketId) {
    return dsl.selectFrom(DSL.table("service_resolution"))
        .where(uuid("ticket_id").eq(ticketId))
        .fetchOne(
            row ->
                new ServiceTicket.Resolution(
                    row.get(uuid("id")),
                    ServiceTicket.Resolution.Type.valueOf(row.get(text("resolution_type"))),
                    row.get(text("outcome")), instant(row, "resolved_at")));
  }

  private static java.time.Instant instant(final Record row, final String name) {
    return row.get(DSL.field(name, OffsetDateTime.class)).toInstant();
  }

  private static java.time.LocalDate localDate(final Record row, final String name) {
    final Object value = row.get(name);
    if (value == null) {
      return null;
    }
    return value instanceof java.time.LocalDate local
        ? local
        : ((java.sql.Date) value).toLocalDate();
  }

  private static Field<UUID> uuid(final String name) {
    return DSL.field(name, UUID.class);
  }

  private static Field<String> text(final String name) {
    return DSL.field(name, String.class);
  }

  private static Field<Integer> integer(final String name) {
    return DSL.field(name, Integer.class);
  }

  private static Field<Long> longField(final String name) {
    return DSL.field(name, Long.class);
  }

  private static Field<Boolean> bool(final String name) {
    return DSL.field(name, Boolean.class);
  }

  private static Field<java.time.LocalDate> date(final String name) {
    return DSL.field(name, java.time.LocalDate.class);
  }

  private static Field<Object> field(final String name) {
    return DSL.field(name);
  }
}
