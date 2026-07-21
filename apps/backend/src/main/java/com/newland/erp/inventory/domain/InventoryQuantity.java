package com.newland.erp.inventory.domain;

import java.math.BigDecimal;

public record InventoryQuantity(BigDecimal value, String uomCode) {
    public InventoryQuantity {
        if (value == null) {
            throw new IllegalArgumentException("Inventory quantity is required.");
        }
        uomCode = required("uomCode", uomCode).toUpperCase();
    }

    public static InventoryQuantity zero(final String uomCode) {
        return new InventoryQuantity(BigDecimal.ZERO, uomCode);
    }

    public InventoryQuantity add(final InventoryQuantity other) {
        ensureSameUom(other);
        return new InventoryQuantity(value.add(other.value), uomCode);
    }

    public InventoryQuantity subtract(final InventoryQuantity other) {
        ensureSameUom(other);
        return new InventoryQuantity(value.subtract(other.value), uomCode);
    }

    public boolean isNegative() {
        return value.signum() < 0;
    }

    public boolean isPositive() {
        return value.signum() > 0;
    }

    private void ensureSameUom(final InventoryQuantity other) {
        if (!uomCode.equals(other.uomCode())) {
            throw new IllegalArgumentException("Inventory quantity UOM mismatch.");
        }
    }

    private static String required(final String name, final String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Inventory quantity " + name + " is required.");
        }
        return value.trim();
    }
}
