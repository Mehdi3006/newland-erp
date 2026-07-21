package com.newland.erp.inventory.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record StockLedgerEntry(UUID id, UUID transactionId, UUID lineId, UUID skuId, InventoryLocation location,
                               InventoryQuantity quantityDelta, InventoryStatus inventoryStatus, String lotCode,
                               String serialCode, LocalDate expiryDate, Instant postedAt) {
    public StockLedgerEntry {
        if (id == null || transactionId == null || lineId == null || skuId == null) {
            throw new IllegalArgumentException("Stock ledger identifiers are required.");
        }
        if (location == null || quantityDelta == null || inventoryStatus == null || postedAt == null) {
            throw new IllegalArgumentException("Stock ledger location, quantity, status and time are required.");
        }
    }
}
