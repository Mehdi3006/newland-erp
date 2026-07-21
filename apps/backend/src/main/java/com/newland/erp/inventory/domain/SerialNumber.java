package com.newland.erp.inventory.domain;

import java.util.UUID;

public record SerialNumber(UUID id, UUID skuId, String serialCode) {
    public SerialNumber {
        if (id == null || skuId == null) {
            throw new IllegalArgumentException("Serial identifiers are required.");
        }
        serialCode = required("serialCode", serialCode).toUpperCase();
    }

    private static String required(final String name, final String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Serial " + name + " is required.");
        }
        return value.trim();
    }
}
