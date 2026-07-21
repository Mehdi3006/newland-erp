package com.newland.erp.procurement.domain;

import java.math.BigDecimal;
import java.util.UUID;

public record ProcurementLine(UUID id, UUID productId, UUID skuId, String skuCode, ProcurementQuantity quantity,
                              BigDecimal unitPrice, UUID taxCategoryId) {
    public ProcurementLine {
        if (id == null || productId == null || skuId == null || quantity == null || !quantity.isPositive()) {
            throw new IllegalArgumentException("Procurement line product, SKU and positive quantity are required.");
        }
        skuCode = required("skuCode", skuCode).toUpperCase();
        if (unitPrice != null && unitPrice.signum() < 0) {
            throw new IllegalArgumentException("Procurement line unit price cannot be negative.");
        }
    }

    private static String required(final String name, final String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Procurement line " + name + " is required.");
        }
        return value.trim();
    }
}
