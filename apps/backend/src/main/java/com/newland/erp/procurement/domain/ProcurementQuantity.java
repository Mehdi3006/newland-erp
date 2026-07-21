package com.newland.erp.procurement.domain;

import java.math.BigDecimal;
import java.util.Objects;

public record ProcurementQuantity(BigDecimal value, String uomCode) {
    public ProcurementQuantity {
        if (value == null || value.signum() < 0) {
            throw new IllegalArgumentException("Procurement quantity cannot be negative.");
        }
        if (uomCode == null || uomCode.isBlank()) {
            throw new IllegalArgumentException("Procurement quantity UOM is required.");
        }
        uomCode = uomCode.trim().toUpperCase();
    }

    public boolean isPositive() {
        return value.signum() > 0;
    }

    public ProcurementQuantity add(final ProcurementQuantity other) {
        requireSameUom(other);
        return new ProcurementQuantity(value.add(other.value()), uomCode);
    }

    public ProcurementQuantity subtract(final ProcurementQuantity other) {
        requireSameUom(other);
        return new ProcurementQuantity(value.subtract(other.value()), uomCode);
    }

    public boolean isGreaterThan(final ProcurementQuantity other) {
        requireSameUom(other);
        return value.compareTo(other.value()) > 0;
    }

    private void requireSameUom(final ProcurementQuantity other) {
        if (other == null || !Objects.equals(uomCode, other.uomCode)) {
            throw new ProcurementConflictException("Procurement quantities must use the same UOM.");
        }
    }
}
