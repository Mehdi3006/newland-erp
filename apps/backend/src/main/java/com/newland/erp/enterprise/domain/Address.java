package com.newland.erp.enterprise.domain;

public record Address(String line1, String line2, String city, String region, String postalCode) {
    public Address {
        line1 = optional(line1, "address.line1", 160);
        line2 = optional(line2, "address.line2", 160);
        city = optional(city, "address.city", 80);
        region = optional(region, "address.region", 80);
        postalCode = optional(postalCode, "address.postalCode", 32);
    }

    private static String optional(final String rawValue, final String field, final int maxLength) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        return TextValue.required(rawValue, field, maxLength);
    }
}
