package com.newland.erp.logistics.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record LandedCostDraft(
    UUID id,
    UUID shipmentId,
    String idempotencyKey,
    String currencyCode,
    AllocationBasis allocationBasis,
    List<CostComponent> components,
    Instant createdAt,
    String actor) {
  public LandedCostDraft {
    Shipment.require(id, "landed-cost id");
    Shipment.require(shipmentId, "shipment id");
    Shipment.require(createdAt, "created at");
    idempotencyKey = Shipment.text(idempotencyKey, "idempotency key");
    currencyCode = Shipment.text(currencyCode, "currency code").toUpperCase();
    Shipment.require(allocationBasis, "allocation basis");
    actor = Shipment.text(actor, "actor");
    components = components == null ? List.of() : List.copyOf(components);
    if (components.isEmpty()) {
      throw new IllegalArgumentException("Landed-cost draft requires cost components.");
    }
  }

  public BigDecimal total() {
    return components.stream().map(CostComponent::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  public enum AllocationBasis {
    VALUE,
    WEIGHT,
    VOLUME,
    QUANTITY
  }

  public record CostComponent(UUID id, String costType, BigDecimal amount, String reference) {
    public CostComponent {
      Shipment.require(id, "cost component id");
      costType = Shipment.text(costType, "cost type").toUpperCase();
      reference = Shipment.text(reference, "cost reference");
      if (amount == null || amount.signum() <= 0) {
        throw new IllegalArgumentException("Landed-cost amount must be positive.");
      }
    }
  }
}
