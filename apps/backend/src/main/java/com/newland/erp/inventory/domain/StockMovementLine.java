package com.newland.erp.inventory.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record StockMovementLine(UUID id, InventoryItemReference item, InventoryLocation fromLocation,
                                InventoryLocation toLocation, InventoryQuantity quantity,
                                InventoryStatus inventoryStatus, String lotCode, String serialCode,
                                LocalDate expiryDate) {
    public StockMovementLine {
        if (id == null) {
            throw new IllegalArgumentException("Stock movement line id is required.");
        }
        if (item == null || quantity == null) {
            throw new IllegalArgumentException("Stock movement item and quantity are required.");
        }
        if (!quantity.isPositive()) {
            throw new IllegalArgumentException("Stock movement quantity must be positive.");
        }
        inventoryStatus = inventoryStatus == null ? InventoryStatus.AVAILABLE : inventoryStatus;
        lotCode = blankToNull(lotCode);
        serialCode = blankToNull(serialCode);
        if (item.trackingPolicy() == TrackingPolicy.LOT && lotCode == null) {
            throw new InventoryConflictException("Lot-controlled item requires a lot code.");
        }
        if (item.trackingPolicy() == TrackingPolicy.SERIAL) {
            if (serialCode == null) {
                throw new InventoryConflictException("Serial-controlled item requires a serial number.");
            }
            if (quantity.value().compareTo(BigDecimal.ONE) != 0) {
                throw new InventoryConflictException("Serial-controlled item quantity must be 1.");
            }
        }
    }

    private static String blankToNull(final String value) {
        return value == null || value.isBlank() ? null : value.trim().toUpperCase();
    }
}
