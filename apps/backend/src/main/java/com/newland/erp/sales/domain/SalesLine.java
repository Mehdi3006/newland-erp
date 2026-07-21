package com.newland.erp.sales.domain;

import java.math.BigDecimal;
import java.util.UUID;

public record SalesLine(UUID id, UUID productId, UUID skuId, String skuCode, SalesQuantity quantity,
                        BigDecimal unitPrice, UUID taxCategoryId) {
    public SalesLine {
        if (id == null || productId == null || skuId == null || quantity == null || !quantity.isPositive()) {
            throw new IllegalArgumentException("Sales line product, SKU and positive quantity are required.");
        }
        skuCode = required("skuCode", skuCode).toUpperCase();
        if (unitPrice != null && unitPrice.signum() < 0) {
            throw new IllegalArgumentException("Sales line commercial terms cannot be negative.");
        }
    }

    static String required(final String name, final String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Sales " + name + " is required.");
        }
        return value.trim();
    }
}
