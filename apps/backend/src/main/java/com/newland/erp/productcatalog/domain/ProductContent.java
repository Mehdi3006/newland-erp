package com.newland.erp.productcatalog.domain;

public record ProductContent(String languageCode, String displayName, String description,
                             String manualReference, String brochureReference) {
    public ProductContent {
        languageCode = required("languageCode", languageCode).toLowerCase();
        displayName = required("displayName", displayName);
        description = description == null ? "" : description.trim();
        manualReference = blankToNull(manualReference);
        brochureReference = blankToNull(brochureReference);
    }

    private static String required(final String name, final String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Product content " + name + " is required.");
        }
        return value.trim();
    }

    private static String blankToNull(final String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
