package com.newland.erp.inventory.domain;

import java.util.UUID;

public record InventoryLocation(UUID warehouseId, UUID zoneId, UUID binId) {
    public InventoryLocation {
        if (warehouseId == null) {
            throw new IllegalArgumentException("Inventory warehouse is required.");
        }
    }
}
