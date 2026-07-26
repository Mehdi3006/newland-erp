package com.newland.erp.logistics.application;

import com.newland.erp.logistics.domain.LandedCostDraft;
import com.newland.erp.logistics.domain.Shipment;
import com.newland.erp.platform.application.integration.PlatformAuditOutboxPort;
import com.newland.erp.procurement.application.integration.ProcurementReferencePort;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public final class LogisticsService {
  private final LogisticsRepository repository;
  private final ProcurementReferencePort procurement;
  private final PlatformAuditOutboxPort platform;
  private final LogisticsSecurityPort security;
  private final Clock clock;

  public LogisticsService(
      final LogisticsRepository logisticsRepository,
      final ProcurementReferencePort procurementPort,
      final PlatformAuditOutboxPort platformPort,
      final LogisticsSecurityPort securityPort,
      final Clock systemClock) {
    repository = logisticsRepository;
    procurement = procurementPort;
    platform = platformPort;
    security = securityPort;
    clock = systemClock;
  }

  @Transactional
  public Shipment createShipment(final CreateShipment command) {
    security.require(command.actor(), "logistics.shipment.manage", command.companyId());
    if (repository.idempotencyKeyExists(command.idempotencyKey())
        || repository.shipmentNumberExists(command.shipmentNumber())) {
      throw new IllegalStateException("Duplicate logistics idempotency key or shipment number.");
    }
    final var order = procurement.requireApprovedPurchaseOrder(command.purchaseOrderId());
    if (!order.companyId().equals(command.companyId())) {
      throw new IllegalArgumentException("Purchase order is outside shipment company scope.");
    }
    final Shipment shipment =
        new Shipment(
            UUID.randomUUID(),
            command.shipmentNumber(),
            command.idempotencyKey(),
            order.purchaseOrderId(),
            order.supplierId(),
            order.companyId(),
            order.branchId(),
            order.warehouseId(),
            command.carrierCode(),
            command.originPortCode(),
            command.destinationPortCode(),
            command.incotermCode(),
            command.estimatedDeparture(),
            command.estimatedArrival(),
            Shipment.Status.DRAFT,
            List.of(),
            List.of(),
            0,
            Instant.now(clock),
            command.actor());
    repository.insertShipment(shipment);
    audit(command.actor(), "LOGISTICS_SHIPMENT_CREATED", shipment.id());
    return shipment;
  }

  @Transactional
  public Shipment book(final UUID shipmentId, final String actor) {
    final Shipment shipment = shipment(shipmentId);
    security.require(actor, "logistics.shipment.manage", shipment.companyId());
    final Shipment booked = repository.updateShipment(shipment.book());
    platform.publishEvent("logistics", "ShipmentBooked", booked.id(),
        Map.of("purchaseOrderId", booked.purchaseOrderId().toString()));
    audit(actor, "LOGISTICS_SHIPMENT_BOOKED", booked.id());
    return booked;
  }

  @Transactional
  public Shipment addContainer(
      final UUID shipmentId, final Shipment.Container container, final String actor) {
    final Shipment shipment = shipment(shipmentId);
    security.require(actor, "logistics.container.manage", shipment.companyId());
    if (repository.containerNumberExists(container.containerNumber())) {
      throw new IllegalStateException("Container number must be globally unique.");
    }
    final Shipment updated = repository.updateShipment(shipment.addContainer(container));
    audit(actor, "LOGISTICS_CONTAINER_ADDED", container.id());
    return updated;
  }

  @Transactional
  public Shipment loadContainer(
      final UUID shipmentId, final UUID containerId, final String actor) {
    final Shipment shipment = shipment(shipmentId);
    security.require(actor, "logistics.container.manage", shipment.companyId());
    final Shipment updated =
        repository.updateShipment(shipment.loadContainer(containerId, Instant.now(clock)));
    platform.publishEvent("logistics", "ContainerLoaded", containerId,
        Map.of("shipmentId", shipmentId.toString()));
    audit(actor, "LOGISTICS_CONTAINER_LOADED", containerId);
    return updated;
  }

  @Transactional
  public Shipment recordMilestone(
      final UUID shipmentId, final Shipment.CustomsMilestone milestone, final String actor) {
    final Shipment shipment = shipment(shipmentId);
    security.require(actor, "logistics.customs.manage", shipment.companyId());
    final Shipment updated = repository.updateShipment(shipment.recordMilestone(milestone));
    if (milestone.type() == Shipment.MilestoneType.CUSTOMS_RELEASED) {
      platform.publishEvent("logistics", "CustomsReleased", shipment.id(),
          Map.of("releaseReference", milestone.reference()));
    }
    audit(actor, "LOGISTICS_CUSTOMS_MILESTONE_RECORDED", milestone.id());
    return updated;
  }

  @Transactional
  public LandedCostDraft createLandedCostDraft(final LandedCostDraft draft) {
    final Shipment shipment = shipment(draft.shipmentId());
    security.require(draft.actor(), "logistics.landed-cost.manage", shipment.companyId());
    if (repository.idempotencyKeyExists(draft.idempotencyKey())) {
      throw new IllegalStateException("Duplicate logistics idempotency key.");
    }
    final LandedCostDraft saved = repository.insertLandedCostDraft(draft);
    audit(draft.actor(), "LOGISTICS_LANDED_COST_DRAFT_CREATED", draft.id());
    return saved;
  }

  @Transactional(readOnly = true)
  public Shipment shipment(final UUID shipmentId) {
    return repository
        .findShipment(shipmentId)
        .orElseThrow(() -> new IllegalArgumentException("Shipment not found."));
  }

  @Transactional(readOnly = true)
  public List<Shipment> shipments(final UUID companyId, final String actor) {
    security.require(actor, "logistics.shipment.read", companyId);
    return repository.listShipments(companyId);
  }

  private void audit(final String actor, final String action, final UUID targetId) {
    platform.recordAudit(actor, action, "ImportLogistics", targetId, Map.of());
  }

  public record CreateShipment(
      String shipmentNumber,
      String idempotencyKey,
      UUID purchaseOrderId,
      UUID companyId,
      String carrierCode,
      String originPortCode,
      String destinationPortCode,
      String incotermCode,
      LocalDate estimatedDeparture,
      LocalDate estimatedArrival,
      String actor) {}
}
