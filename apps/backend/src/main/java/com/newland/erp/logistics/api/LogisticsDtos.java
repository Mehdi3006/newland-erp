package com.newland.erp.logistics.api;

import com.newland.erp.logistics.application.LogisticsService;
import com.newland.erp.logistics.domain.LandedCostDraft;
import com.newland.erp.logistics.domain.Shipment;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class LogisticsDtos {
  public record CreateShipmentRequest(
      @NotBlank String shipmentNumber,
      @NotBlank String idempotencyKey,
      @NotNull UUID purchaseOrderId,
      @NotNull UUID companyId,
      @NotBlank String carrierCode,
      @NotBlank String originPortCode,
      @NotBlank String destinationPortCode,
      @NotBlank String incotermCode,
      @NotNull LocalDate estimatedDeparture,
      @NotNull LocalDate estimatedArrival) {
    LogisticsService.CreateShipment command(final String actor) {
      return new LogisticsService.CreateShipment(
          shipmentNumber, idempotencyKey, purchaseOrderId, companyId, carrierCode,
          originPortCode, destinationPortCode, incotermCode, estimatedDeparture,
          estimatedArrival, actor);
    }
  }

  public record AddContainerRequest(
      @NotBlank String containerNumber,
      @NotBlank String containerType,
      @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal grossWeight,
      @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal volumeCbm) {
    Shipment.Container domain() {
      return new Shipment.Container(
          UUID.randomUUID(), containerNumber, containerType, grossWeight, volumeCbm, null);
    }
  }

  public record MilestoneRequest(
      @NotBlank String type,
      @NotBlank String reference,
      @NotNull Instant occurredAt,
      String notes) {
    Shipment.CustomsMilestone domain() {
      return new Shipment.CustomsMilestone(
          UUID.randomUUID(), Shipment.MilestoneType.valueOf(type), reference, occurredAt, notes);
    }
  }

  public record LandedCostRequest(
      @NotBlank String idempotencyKey,
      @NotBlank String currencyCode,
      @NotBlank String allocationBasis,
      @NotNull List<CostComponentRequest> components) {
    LandedCostDraft domain(final UUID shipmentId, final String actor) {
      return new LandedCostDraft(
          UUID.randomUUID(), shipmentId, idempotencyKey, currencyCode,
          LandedCostDraft.AllocationBasis.valueOf(allocationBasis),
          components.stream().map(CostComponentRequest::domain).toList(),
          Instant.now(), actor);
    }
  }

  public record CostComponentRequest(
      @NotBlank String costType,
      @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal amount,
      @NotBlank String reference) {
    LandedCostDraft.CostComponent domain() {
      return new LandedCostDraft.CostComponent(UUID.randomUUID(), costType, amount, reference);
    }
  }

  public record ShipmentResponse(
      UUID id,
      String shipmentNumber,
      UUID purchaseOrderId,
      UUID supplierId,
      UUID companyId,
      String status,
      int containerCount,
      List<MilestoneResponse> milestones,
      long version) {
    static ShipmentResponse from(final Shipment shipment) {
      return new ShipmentResponse(
          shipment.id(), shipment.shipmentNumber(), shipment.purchaseOrderId(),
          shipment.supplierId(), shipment.companyId(), shipment.status().name(),
          shipment.containers().size(),
          shipment.milestones().stream().map(MilestoneResponse::from).toList(),
          shipment.version());
    }
  }

  public record MilestoneResponse(String type, String reference, Instant occurredAt, String notes) {
    static MilestoneResponse from(final Shipment.CustomsMilestone milestone) {
      return new MilestoneResponse(
          milestone.type().name(), milestone.reference(), milestone.occurredAt(), milestone.notes());
    }
  }

  public record LandedCostResponse(
      UUID id, UUID shipmentId, String currencyCode, String allocationBasis, BigDecimal total) {
    static LandedCostResponse from(final LandedCostDraft draft) {
      return new LandedCostResponse(
          draft.id(), draft.shipmentId(), draft.currencyCode(),
          draft.allocationBasis().name(), draft.total());
    }
  }

  private LogisticsDtos() {}
}
