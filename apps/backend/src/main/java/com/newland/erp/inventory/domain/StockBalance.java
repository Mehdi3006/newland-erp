package com.newland.erp.inventory.domain;

import java.util.UUID;

public record StockBalance(UUID id, UUID skuId, InventoryLocation location, InventoryStatus inventoryStatus,
                           InventoryQuantity onHandQuantity, InventoryQuantity reservedQuantity,
                           InventoryQuantity inTransitQuantity, InventoryQuantity damagedQuantity,
                           InventoryQuantity quarantineQuantity, long version) {
    public StockBalance {
        if (id == null || skuId == null || location == null || inventoryStatus == null) {
            throw new IllegalArgumentException("Stock balance identifiers are required.");
        }
        if (onHandQuantity == null || reservedQuantity == null || inTransitQuantity == null
                || damagedQuantity == null || quarantineQuantity == null) {
            throw new IllegalArgumentException("Stock balance quantities are required.");
        }
        if (version < 0) {
            throw new IllegalArgumentException("Stock balance version cannot be negative.");
        }
    }

    public InventoryQuantity availableQuantity() {
        return onHandQuantity.subtract(reservedQuantity).subtract(damagedQuantity).subtract(quarantineQuantity);
    }
}
