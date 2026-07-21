package com.newland.erp.sales.domain;

import java.math.BigDecimal;
import java.util.Objects;

public record SalesQuantity(BigDecimal value, String uomCode) {
    public SalesQuantity {
        if (value == null || value.signum() < 0) {
            throw new IllegalArgumentException("Sales quantity cannot be negative.");
        }
        if (uomCode == null || uomCode.isBlank()) {
            throw new IllegalArgumentException("Sales quantity UOM is required.");
        }
        uomCode = uomCode.trim().toUpperCase();
    }

    public boolean isPositive() {
        return value.signum() > 0;
    }

    public SalesQuantity add(final SalesQuantity other) {
        sameUom(other);
        return new SalesQuantity(value.add(other.value()), uomCode);
    }

    public SalesQuantity subtract(final SalesQuantity other) {
        sameUom(other);
        return new SalesQuantity(value.subtract(other.value()), uomCode);
    }

    public boolean isGreaterThan(final SalesQuantity other) {
        sameUom(other);
        return value.compareTo(other.value()) > 0;
    }

    private void sameUom(final SalesQuantity other) {
        if (other == null || !Objects.equals(uomCode, other.uomCode)) {
            throw new SalesConflictException("Sales quantities must use the same UOM.");
        }
    }
}
