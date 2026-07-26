package com.newland.erp.logistics.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record Shipment(
    UUID id,
    String shipmentNumber,
    String idempotencyKey,
    UUID purchaseOrderId,
    UUID supplierId,
    UUID companyId,
    UUID branchId,
    UUID warehouseId,
    String carrierCode,
    String originPortCode,
    String destinationPortCode,
    String incotermCode,
    LocalDate estimatedDeparture,
    LocalDate estimatedArrival,
    Status status,
    List<Container> containers,
    List<CustomsMilestone> milestones,
    long version,
    Instant createdAt,
    String actor) {
  public Shipment {
    require(id, "shipment id");
    require(purchaseOrderId, "purchase order id");
    require(supplierId, "supplier id");
    require(companyId, "company id");
    require(branchId, "branch id");
    require(warehouseId, "warehouse id");
    require(status, "status");
    require(createdAt, "created at");
    shipmentNumber = text(shipmentNumber, "shipment number").toUpperCase();
    idempotencyKey = text(idempotencyKey, "idempotency key");
    carrierCode = text(carrierCode, "carrier code").toUpperCase();
    originPortCode = text(originPortCode, "origin port code").toUpperCase();
    destinationPortCode = text(destinationPortCode, "destination port code").toUpperCase();
    incotermCode = text(incotermCode, "Incoterm code").toUpperCase();
    actor = text(actor, "actor");
    containers = containers == null ? List.of() : List.copyOf(containers);
    milestones = milestones == null ? List.of() : List.copyOf(milestones);
    if (estimatedDeparture == null
        || estimatedArrival == null
        || estimatedArrival.isBefore(estimatedDeparture)
        || version < 0) {
      throw new IllegalArgumentException("Shipment dates and version are invalid.");
    }
  }

  public Shipment book() {
    if (status != Status.DRAFT) {
      throw new IllegalStateException("Only draft shipments can be booked.");
    }
    return copy(Status.BOOKED, containers, milestones);
  }

  public Shipment addContainer(final Container container) {
    if (status != Status.BOOKED && status != Status.IN_TRANSIT) {
      throw new IllegalStateException("Containers require a booked shipment.");
    }
    if (containers.stream().anyMatch(existing -> existing.containerNumber().equals(container.containerNumber()))) {
      throw new IllegalStateException("Container number already belongs to this shipment.");
    }
    final var next = new java.util.ArrayList<>(containers);
    next.add(container);
    return copy(status, next, milestones);
  }

  public Shipment loadContainer(final UUID containerId, final Instant loadedAt) {
    if (status != Status.BOOKED && status != Status.IN_TRANSIT) {
      throw new IllegalStateException("Container loading requires a booked shipment.");
    }
    final List<Container> next =
        containers.stream()
            .map(container -> container.id().equals(containerId) ? container.load(loadedAt) : container)
            .toList();
    if (next.equals(containers)) {
      throw new IllegalArgumentException("Container not found.");
    }
    return copy(Status.IN_TRANSIT, next, milestones);
  }

  public Shipment recordMilestone(final CustomsMilestone milestone) {
    if (status == Status.DELIVERED || status == Status.CANCELLED) {
      throw new IllegalStateException("Terminal shipments cannot accept customs milestones.");
    }
    if (status != Status.IN_TRANSIT
        && status != Status.CUSTOMS_HOLD
        && status != Status.CUSTOMS_RELEASED) {
      throw new IllegalStateException("Customs milestones require an in-transit shipment.");
    }
    if (milestones.stream().anyMatch(existing -> existing.reference().equals(milestone.reference()))) {
      throw new IllegalStateException("Customs milestone reference must be unique.");
    }
    validateMilestoneTransition(milestone);
    final var next = new java.util.ArrayList<>(milestones);
    next.add(milestone);
    final Status nextStatus =
        switch (milestone.type()) {
          case CUSTOMS_HOLD -> Status.CUSTOMS_HOLD;
          case CUSTOMS_RELEASED -> Status.CUSTOMS_RELEASED;
          case INLAND_DELIVERY -> Status.DELIVERED;
          default -> status;
        };
    return copy(nextStatus, containers, next);
  }

  private void validateMilestoneTransition(final CustomsMilestone milestone) {
    final MilestoneType previous =
        milestones.isEmpty() ? null : milestones.getLast().type();
    final boolean allowed =
        switch (milestone.type()) {
          case DEPARTED -> previous == null && status == Status.IN_TRANSIT;
          case ARRIVED_PORT -> previous == MilestoneType.DEPARTED;
          case CUSTOMS_FILED -> previous == MilestoneType.ARRIVED_PORT;
          case CUSTOMS_HOLD -> previous == MilestoneType.CUSTOMS_FILED;
          case CUSTOMS_RELEASED ->
              previous == MilestoneType.CUSTOMS_FILED
                  || previous == MilestoneType.CUSTOMS_HOLD;
          case INLAND_DELIVERY ->
              previous == MilestoneType.CUSTOMS_RELEASED
                  && status == Status.CUSTOMS_RELEASED;
        };
    if (!allowed) {
      throw new IllegalStateException(
          "Invalid customs milestone transition from "
              + (previous == null ? status : previous)
              + " to "
              + milestone.type()
              + ".");
    }
    if (previous != null
        && milestone.occurredAt().isBefore(milestones.getLast().occurredAt())) {
      throw new IllegalStateException("Customs milestone time cannot move backwards.");
    }
  }

  private Shipment copy(
      final Status nextStatus,
      final List<Container> nextContainers,
      final List<CustomsMilestone> nextMilestones) {
    return new Shipment(
        id, shipmentNumber, idempotencyKey, purchaseOrderId, supplierId, companyId, branchId,
        warehouseId, carrierCode, originPortCode, destinationPortCode, incotermCode,
        estimatedDeparture, estimatedArrival, nextStatus, nextContainers, nextMilestones,
        version + 1, createdAt, actor);
  }

  public enum Status {
    DRAFT,
    BOOKED,
    IN_TRANSIT,
    CUSTOMS_HOLD,
    CUSTOMS_RELEASED,
    DELIVERED,
    CANCELLED
  }

  public record Container(
      UUID id,
      String containerNumber,
      String containerType,
      java.math.BigDecimal grossWeight,
      java.math.BigDecimal volumeCbm,
      Instant loadedAt) {
    public Container {
      require(id, "container id");
      containerNumber = text(containerNumber, "container number").toUpperCase();
      containerType = text(containerType, "container type").toUpperCase();
      if (grossWeight == null
          || grossWeight.signum() <= 0
          || volumeCbm == null
          || volumeCbm.signum() <= 0) {
        throw new IllegalArgumentException("Container weight and volume must be positive.");
      }
    }

    Container load(final Instant time) {
      if (loadedAt != null) {
        throw new IllegalStateException("Container is already loaded.");
      }
      return new Container(id, containerNumber, containerType, grossWeight, volumeCbm,
          java.util.Objects.requireNonNull(time));
    }
  }

  public record CustomsMilestone(
      UUID id, MilestoneType type, String reference, Instant occurredAt, String notes) {
    public CustomsMilestone {
      require(id, "milestone id");
      require(type, "milestone type");
      require(occurredAt, "milestone time");
      reference = text(reference, "milestone reference").toUpperCase();
      notes = notes == null ? "" : notes.trim();
    }
  }

  public enum MilestoneType {
    DEPARTED,
    ARRIVED_PORT,
    CUSTOMS_FILED,
    CUSTOMS_HOLD,
    CUSTOMS_RELEASED,
    INLAND_DELIVERY
  }

  static String text(final String value, final String name) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(name + " is required.");
    }
    return value.trim();
  }

  static void require(final Object value, final String name) {
    if (value == null) {
      throw new IllegalArgumentException(name + " is required.");
    }
  }
}
