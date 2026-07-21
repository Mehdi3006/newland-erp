package com.newland.erp.inventory.domain;

import java.time.LocalDate;
import java.util.UUID;

public record Lot(UUID id, UUID skuId, String lotCode, LocalDate expiryDate) {
    public Lot {
        if (id == null || skuId == null) {
            throw new IllegalArgumentException("Lot identifiers are required.");
        }
        lotCode = required("lotCode", lotCode).toUpperCase();
    }

    public boolean isExpired(final LocalDate businessDate) {
        return expiryDate != null && expiryDate.isBefore(businessDate);
    }

    private static String required(final String name, final String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Lot " + name + " is required.");
        }
        return value.trim();
    }
}
