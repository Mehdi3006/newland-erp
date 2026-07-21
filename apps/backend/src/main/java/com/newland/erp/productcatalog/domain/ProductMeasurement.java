package com.newland.erp.productcatalog.domain;

import java.math.BigDecimal;

public record ProductMeasurement(BigDecimal value, String unitCode, BigDecimal normalizedValue,
                                 String normalizedUnitCode) {
    public ProductMeasurement {
        if (value == null || value.signum() < 0) {
            throw new IllegalArgumentException("Measurement value must be zero or positive.");
        }
        unitCode = required("unitCode", unitCode);
        normalizedValue = normalizedValue == null ? value : normalizedValue;
        normalizedUnitCode = normalizedUnitCode == null || normalizedUnitCode.isBlank()
                ? unitCode : normalizedUnitCode.trim().toUpperCase();
    }

    private static String required(final String name, final String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Measurement " + name + " is required.");
        }
        return value.trim().toUpperCase();
    }
}
