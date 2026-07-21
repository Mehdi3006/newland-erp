package com.newland.erp.inventory.domain;

import java.time.Instant;
import java.util.UUID;

public record Reservation(UUID id, UUID skuId, InventoryLocation location, InventoryQuantity quantity,
                          String idempotencyKey, boolean released, Instant createdAt, Instant releasedAt) {
    public Reservation {
        if (id == null || skuId == null || location == null || quantity == null || createdAt == null) {
            throw new IllegalArgumentException("Reservation identifiers, quantity and creation time are required.");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Reservation idempotency key is required.");
        }
        idempotencyKey = idempotencyKey.trim();
        if (!quantity.isPositive()) {
            throw new IllegalArgumentException("Reservation quantity must be positive.");
        }
    }
}
