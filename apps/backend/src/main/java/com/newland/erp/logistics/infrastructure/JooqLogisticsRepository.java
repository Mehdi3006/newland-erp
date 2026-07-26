package com.newland.erp.logistics.infrastructure;

import com.newland.erp.logistics.application.LogisticsRepository;
import com.newland.erp.logistics.domain.LandedCostDraft;
import com.newland.erp.logistics.domain.Shipment;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

@Repository
public final class JooqLogisticsRepository implements LogisticsRepository {
  private final DSLContext dsl;

  public JooqLogisticsRepository(final DSLContext dslContext) {
    dsl = dslContext;
  }

  @Override
  public boolean shipmentNumberExists(final String number) {
    return dsl.fetchExists(
        DSL.table("logistics_shipment"),
        DSL.field("shipment_number", String.class).eq(number.toUpperCase()));
  }

  @Override
  public boolean containerNumberExists(final String number) {
    return dsl.fetchExists(
        DSL.table("logistics_container"),
        DSL.field("container_number", String.class).eq(number.toUpperCase()));
  }

  @Override
  public boolean insertShipmentIfAbsent(final Shipment shipment) {
    return dsl.insertInto(DSL.table("logistics_shipment"))
        .columns(
            field("id"), field("shipment_number"), field("idempotency_key"),
            field("purchase_order_id"), field("supplier_id"), field("company_id"),
            field("branch_id"), field("warehouse_id"), field("carrier_code"),
            field("origin_port_code"), field("destination_port_code"), field("incoterm_code"),
            field("estimated_departure"), field("estimated_arrival"), field("status"),
            field("version"), field("created_at"), field("actor"))
        .values(
            shipment.id(), shipment.shipmentNumber(), shipment.idempotencyKey(),
            shipment.purchaseOrderId(), shipment.supplierId(), shipment.companyId(),
            shipment.branchId(), shipment.warehouseId(), shipment.carrierCode(),
            shipment.originPortCode(), shipment.destinationPortCode(), shipment.incotermCode(),
            shipment.estimatedDeparture(), shipment.estimatedArrival(), shipment.status().name(),
            shipment.version(), shipment.createdAt(), shipment.actor())
        .onConflict(DSL.field("idempotency_key"))
        .doNothing()
        .execute() == 1;
  }

  @Override
  public Optional<Shipment> findShipmentByIdempotencyKey(final String idempotencyKey) {
    return dsl.selectFrom(DSL.table("logistics_shipment"))
        .where(DSL.field("idempotency_key", String.class).eq(idempotencyKey))
        .fetchOptional(this::shipment);
  }

  @Override
  public Shipment updateShipment(final Shipment shipment) {
    final int updated =
        dsl.update(DSL.table("logistics_shipment"))
            .set(DSL.field("status", String.class), shipment.status().name())
            .set(DSL.field("version", Long.class), shipment.version())
            .where(DSL.field("id", UUID.class).eq(shipment.id()))
            .and(DSL.field("version", Long.class).eq(shipment.version() - 1))
            .execute();
    if (updated != 1) {
      throw new IllegalStateException("Shipment was modified concurrently.");
    }
    persistContainers(shipment);
    persistMilestones(shipment);
    return shipment;
  }

  @Override
  public Optional<Shipment> findShipment(final UUID shipmentId) {
    return dsl.selectFrom(DSL.table("logistics_shipment"))
        .where(DSL.field("id", UUID.class).eq(shipmentId))
        .fetchOptional(this::shipment);
  }

  @Override
  public List<Shipment> listShipments(final UUID companyId) {
    return dsl.selectFrom(DSL.table("logistics_shipment"))
        .where(DSL.field("company_id", UUID.class).eq(companyId))
        .orderBy(DSL.field("created_at").desc())
        .fetch(this::shipment);
  }

  @Override
  public boolean insertLandedCostDraftIfAbsent(final LandedCostDraft draft) {
    final int inserted =
        dsl.insertInto(DSL.table("logistics_landed_cost_draft"))
        .columns(
            field("id"), field("shipment_id"), field("idempotency_key"), field("currency_code"),
            field("allocation_basis"), field("total_amount"), field("created_at"), field("actor"))
        .values(
            draft.id(), draft.shipmentId(), draft.idempotencyKey(), draft.currencyCode(),
            draft.allocationBasis().name(), draft.total(), draft.createdAt(), draft.actor())
            .onConflict(DSL.field("idempotency_key"))
            .doNothing()
            .execute();
    if (inserted == 0) {
      return false;
    }
    for (final var component : draft.components()) {
      dsl.insertInto(DSL.table("logistics_landed_cost_component"))
          .columns(field("id"), field("draft_id"), field("cost_type"), field("amount"),
              field("reference"))
          .values(component.id(), draft.id(), component.costType(), component.amount(),
              component.reference())
          .execute();
    }
    return true;
  }

  @Override
  public Optional<LandedCostDraft> findLandedCostDraft(final UUID draftId) {
    return dsl.selectFrom(DSL.table("logistics_landed_cost_draft"))
        .where(DSL.field("id", UUID.class).eq(draftId))
        .fetchOptional(this::landedCostDraft);
  }

  @Override
  public Optional<LandedCostDraft> findLandedCostDraftByIdempotencyKey(
      final String idempotencyKey) {
    return dsl.selectFrom(DSL.table("logistics_landed_cost_draft"))
        .where(DSL.field("idempotency_key", String.class).eq(idempotencyKey))
        .fetchOptional(row -> landedCostDraft(row));
  }

  private Shipment shipment(final Record row) {
    final UUID id = row.get("id", UUID.class);
    return new Shipment(
        id,
        row.get("shipment_number", String.class),
        row.get("idempotency_key", String.class),
        row.get("purchase_order_id", UUID.class),
        row.get("supplier_id", UUID.class),
        row.get("company_id", UUID.class),
        row.get("branch_id", UUID.class),
        row.get("warehouse_id", UUID.class),
        row.get("carrier_code", String.class),
        row.get("origin_port_code", String.class),
        row.get("destination_port_code", String.class),
        row.get("incoterm_code", String.class),
        row.get("estimated_departure", java.time.LocalDate.class),
        row.get("estimated_arrival", java.time.LocalDate.class),
        Shipment.Status.valueOf(row.get("status", String.class)),
        containers(id),
        milestones(id),
        row.get("version", Long.class),
        row.get("created_at", OffsetDateTime.class).toInstant(),
        row.get("actor", String.class));
  }

  private LandedCostDraft landedCostDraft(final Record row) {
    final UUID draftId = row.get("id", UUID.class);
    return new LandedCostDraft(
        draftId,
        row.get("shipment_id", UUID.class),
        row.get("idempotency_key", String.class),
        row.get("currency_code", String.class),
        LandedCostDraft.AllocationBasis.valueOf(row.get("allocation_basis", String.class)),
        components(draftId),
        row.get("created_at", OffsetDateTime.class).toInstant(),
        row.get("actor", String.class));
  }

  private void persistContainers(final Shipment shipment) {
    for (final var container : shipment.containers()) {
      dsl.insertInto(DSL.table("logistics_container"))
          .columns(
              field("id"), field("shipment_id"), field("container_number"), field("container_type"),
              field("gross_weight"), field("volume_cbm"), field("loaded_at"))
          .values(
              container.id(), shipment.id(), container.containerNumber(), container.containerType(),
              container.grossWeight(), container.volumeCbm(), container.loadedAt())
          .onConflict(DSL.field("id"))
          .doUpdate()
          .set(DSL.field("loaded_at"), container.loadedAt())
          .execute();
    }
  }

  private void persistMilestones(final Shipment shipment) {
    for (final var milestone : shipment.milestones()) {
      dsl.insertInto(DSL.table("logistics_customs_milestone"))
          .columns(
              field("id"), field("shipment_id"), field("milestone_type"), field("reference"),
              field("occurred_at"), field("notes"))
          .values(
              milestone.id(), shipment.id(), milestone.type().name(), milestone.reference(),
              milestone.occurredAt(), milestone.notes())
          .onConflictDoNothing()
          .execute();
    }
  }

  private List<Shipment.Container> containers(final UUID shipmentId) {
    return dsl.selectFrom(DSL.table("logistics_container"))
        .where(DSL.field("shipment_id", UUID.class).eq(shipmentId))
        .fetch(
            row ->
                new Shipment.Container(
                    row.get("id", UUID.class),
                    row.get("container_number", String.class),
                    row.get("container_type", String.class),
                    row.get("gross_weight", BigDecimal.class),
                    row.get("volume_cbm", BigDecimal.class),
                    instant(row, "loaded_at")));
  }

  private List<Shipment.CustomsMilestone> milestones(final UUID shipmentId) {
    return dsl.selectFrom(DSL.table("logistics_customs_milestone"))
        .where(DSL.field("shipment_id", UUID.class).eq(shipmentId))
        .orderBy(DSL.field("occurred_at"))
        .fetch(
            row ->
                new Shipment.CustomsMilestone(
                    row.get("id", UUID.class),
                    Shipment.MilestoneType.valueOf(row.get("milestone_type", String.class)),
                    row.get("reference", String.class),
                    instant(row, "occurred_at"),
                    row.get("notes", String.class)));
  }

  private List<LandedCostDraft.CostComponent> components(final UUID draftId) {
    return dsl.selectFrom(DSL.table("logistics_landed_cost_component"))
        .where(DSL.field("draft_id", UUID.class).eq(draftId))
        .fetch(
            row ->
                new LandedCostDraft.CostComponent(
                    row.get("id", UUID.class),
                    row.get("cost_type", String.class),
                    row.get("amount", BigDecimal.class),
                    row.get("reference", String.class)));
  }

  private static java.time.Instant instant(final Record row, final String field) {
    final OffsetDateTime value = row.get(field, OffsetDateTime.class);
    return value == null ? null : value.toInstant();
  }

  private static org.jooq.Field<Object> field(final String name) {
    return DSL.field(name);
  }
}
