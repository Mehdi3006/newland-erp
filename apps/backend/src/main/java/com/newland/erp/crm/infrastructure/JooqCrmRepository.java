package com.newland.erp.crm.infrastructure;

import com.newland.erp.crm.application.CrmRepository;
import com.newland.erp.crm.domain.Activity;
import com.newland.erp.crm.domain.Lead;
import com.newland.erp.crm.domain.Opportunity;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

@Repository
public final class JooqCrmRepository implements CrmRepository {
  private final DSLContext dsl;

  public JooqCrmRepository(final DSLContext dslContext) {
    dsl = dslContext;
  }

  @Override
  public boolean insertLeadIfAbsent(final Lead lead) {
    return dsl.insertInto(DSL.table("crm_lead"))
            .columns(
                field("id"), field("idempotency_key"), field("company_id"), field("branch_id"),
                field("owner_id"), field("lead_number"), field("organization_name"),
                field("contact_name"), field("email"), field("phone"), field("source"),
                field("status"), field("disposition_reason"), field("version"),
                field("created_at"), field("updated_at"), field("actor"))
            .values(
                lead.id(), lead.idempotencyKey(), lead.companyId(), lead.branchId(), lead.ownerId(),
                lead.leadNumber(), lead.organizationName(), lead.contactName(), lead.email(),
                lead.phone(), lead.source(), lead.status().name(), lead.dispositionReason(),
                lead.version(), lead.createdAt(), lead.updatedAt(), lead.actor())
            .onConflict(DSL.field("idempotency_key"))
            .doNothing()
            .execute()
        == 1;
  }

  @Override
  public Optional<Lead> findLead(final UUID leadId) {
    return dsl.selectFrom(DSL.table("crm_lead"))
        .where(uuid("id").eq(leadId))
        .fetchOptional(this::lead);
  }

  @Override
  public Optional<Lead> findLeadByIdempotencyKey(final String idempotencyKey) {
    return dsl.selectFrom(DSL.table("crm_lead"))
        .where(text("idempotency_key").eq(idempotencyKey))
        .fetchOptional(this::lead);
  }

  @Override
  public Lead updateLead(final Lead lead) {
    final int updated =
        dsl.update(DSL.table("crm_lead"))
            .set(text("status"), lead.status().name())
            .set(text("disposition_reason"), lead.dispositionReason())
            .set(longField("version"), lead.version())
            .set(field("updated_at"), lead.updatedAt())
            .where(uuid("id").eq(lead.id()))
            .and(longField("version").eq(lead.version() - 1))
            .execute();
    if (updated != 1) {
      throw new IllegalStateException("CRM lead was modified concurrently.");
    }
    return lead;
  }

  @Override
  public boolean insertOpportunityIfAbsent(final Opportunity opportunity) {
    return dsl.insertInto(DSL.table("crm_opportunity"))
            .columns(
                field("id"), field("idempotency_key"), field("company_id"), field("branch_id"),
                field("owner_id"), field("lead_id"), field("customer_id"),
                field("opportunity_number"), field("name"), field("stage"),
                field("estimated_value"), field("currency_code"), field("probability_percent"),
                field("expected_close_date"), field("closure_reason"), field("version"),
                field("created_at"), field("updated_at"), field("actor"))
            .values(
                opportunity.id(), opportunity.idempotencyKey(), opportunity.companyId(),
                opportunity.branchId(), opportunity.ownerId(), opportunity.leadId(),
                opportunity.customerId(), opportunity.opportunityNumber(), opportunity.name(),
                opportunity.stage().name(), opportunity.estimatedValue(),
                opportunity.currencyCode(), opportunity.probabilityPercent(),
                opportunity.expectedCloseDate(), opportunity.closureReason(),
                opportunity.version(), opportunity.createdAt(), opportunity.updatedAt(),
                opportunity.actor())
            .onConflict(DSL.field("idempotency_key"))
            .doNothing()
            .execute()
        == 1;
  }

  @Override
  public Optional<Opportunity> findOpportunity(final UUID opportunityId) {
    return dsl.selectFrom(DSL.table("crm_opportunity"))
        .where(uuid("id").eq(opportunityId))
        .fetchOptional(this::opportunity);
  }

  @Override
  public Optional<Opportunity> findOpportunityByIdempotencyKey(final String idempotencyKey) {
    return dsl.selectFrom(DSL.table("crm_opportunity"))
        .where(text("idempotency_key").eq(idempotencyKey))
        .fetchOptional(this::opportunity);
  }

  @Override
  public Opportunity updateOpportunity(final Opportunity opportunity) {
    final int updated =
        dsl.update(DSL.table("crm_opportunity"))
            .set(text("stage"), opportunity.stage().name())
            .set(integer("probability_percent"), opportunity.probabilityPercent())
            .set(text("closure_reason"), opportunity.closureReason())
            .set(longField("version"), opportunity.version())
            .set(field("updated_at"), opportunity.updatedAt())
            .where(uuid("id").eq(opportunity.id()))
            .and(longField("version").eq(opportunity.version() - 1))
            .execute();
    if (updated != 1) {
      throw new IllegalStateException("CRM opportunity was modified concurrently.");
    }
    return opportunity;
  }

  @Override
  public boolean insertActivityIfAbsent(final Activity activity) {
    return dsl.insertInto(DSL.table("crm_activity"))
            .columns(
                field("id"), field("idempotency_key"), field("company_id"), field("customer_id"),
                field("lead_id"), field("opportunity_id"), field("activity_type"),
                field("subject"), field("details"), field("occurred_at"), field("follow_up_at"),
                field("actor"))
            .values(
                activity.id(), activity.idempotencyKey(), activity.companyId(),
                activity.customerId(), activity.leadId(), activity.opportunityId(),
                activity.type().name(), activity.subject(), activity.details(),
                activity.occurredAt(), activity.followUpAt(), activity.actor())
            .onConflict(DSL.field("idempotency_key"))
            .doNothing()
            .execute()
        == 1;
  }

  @Override
  public Optional<Activity> findActivityByIdempotencyKey(final String idempotencyKey) {
    return dsl.selectFrom(DSL.table("crm_activity"))
        .where(text("idempotency_key").eq(idempotencyKey))
        .fetchOptional(this::activity);
  }

  @Override
  public List<Activity> listCustomerActivities(
      final UUID companyId, final UUID customerId) {
    return dsl.selectFrom(DSL.table("crm_activity"))
        .where(uuid("company_id").eq(companyId).and(uuid("customer_id").eq(customerId)))
        .orderBy(instant("occurred_at").desc(), uuid("id"))
        .fetch(this::activity);
  }

  private Lead lead(final Record row) {
    return new Lead(
        row.get(uuid("id")), row.get(text("idempotency_key")), row.get(uuid("company_id")),
        row.get(uuid("branch_id")), row.get(uuid("owner_id")), row.get(text("lead_number")),
        row.get(text("organization_name")), row.get(text("contact_name")), row.get(text("email")),
        row.get(text("phone")), row.get(text("source")),
        Lead.Status.valueOf(row.get(text("status"))), row.get(text("disposition_reason")),
        row.get(longField("version")), valueInstant(row, "created_at"),
        valueInstant(row, "updated_at"), row.get(text("actor")));
  }

  private Opportunity opportunity(final Record row) {
    return new Opportunity(
        row.get(uuid("id")), row.get(text("idempotency_key")), row.get(uuid("company_id")),
        row.get(uuid("branch_id")), row.get(uuid("owner_id")), row.get(uuid("lead_id")),
        row.get(uuid("customer_id")), row.get(text("opportunity_number")), row.get(text("name")),
        Opportunity.Stage.valueOf(row.get(text("stage"))), row.get(decimal("estimated_value")),
        row.get(text("currency_code")), row.get(integer("probability_percent")),
        localDate(row, "expected_close_date"),
        row.get(text("closure_reason")), row.get(longField("version")),
        valueInstant(row, "created_at"), valueInstant(row, "updated_at"), row.get(text("actor")));
  }

  private Activity activity(final Record row) {
    return new Activity(
        row.get(uuid("id")), row.get(text("idempotency_key")), row.get(uuid("company_id")),
        row.get(uuid("customer_id")), row.get(uuid("lead_id")), row.get(uuid("opportunity_id")),
        Activity.Type.valueOf(row.get(text("activity_type"))), row.get(text("subject")),
        row.get(text("details")), valueInstant(row, "occurred_at"),
        nullableInstant(row, "follow_up_at"), row.get(text("actor")));
  }

  private static java.time.Instant valueInstant(final Record row, final String name) {
    return row.get(instant(name)).toInstant();
  }

  private static java.time.Instant nullableInstant(final Record row, final String name) {
    final OffsetDateTime value = row.get(instant(name));
    return value == null ? null : value.toInstant();
  }

  private static java.time.LocalDate localDate(final Record row, final String name) {
    final Object value = row.get(name);
    if (value instanceof java.time.LocalDate date) {
      return date;
    }
    return ((java.sql.Date) value).toLocalDate();
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

  private static Field<BigDecimal> decimal(final String name) {
    return DSL.field(name, BigDecimal.class);
  }

  private static Field<OffsetDateTime> instant(final String name) {
    return DSL.field(name, OffsetDateTime.class);
  }

  private static Field<Object> field(final String name) {
    return DSL.field(name);
  }
}
