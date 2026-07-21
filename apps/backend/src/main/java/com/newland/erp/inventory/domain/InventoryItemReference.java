package com.newland.erp.inventory.domain;

import java.util.UUID;

public record InventoryItemReference(UUID productId, UUID skuId, String skuCode, String uomCode,
                                     TrackingPolicy trackingPolicy) {
    public InventoryItemReference {
        if (productId == null || skuId == null) {
            throw new IllegalArgumentException("Inventory item product and SKU references are required.");
        }
        skuCode = required("skuCode", skuCode).toUpperCase();
        uomCode = required("uomCode", uomCode).toUpperCase();
        trackingPolicy = trackingPolicy == null ? TrackingPolicy.NONE : trackingPolicy;
    }

    private static String required(final String name, final String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Inventory item " + name + " is required.");
        }
        return value.trim();
    }
}
