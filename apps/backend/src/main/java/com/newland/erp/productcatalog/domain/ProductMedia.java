package com.newland.erp.productcatalog.domain;

import java.util.UUID;

public record ProductMedia(UUID attachmentId, String mediaType, String languageCode, boolean primaryMedia) {
    public ProductMedia {
        if (attachmentId == null) {
            throw new IllegalArgumentException("Product media attachment id is required.");
        }
        mediaType = required("mediaType", mediaType);
        languageCode = languageCode == null || languageCode.isBlank() ? null : languageCode.trim().toLowerCase();
    }

    private static String required(final String name, final String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Product media " + name + " is required.");
        }
        return value.trim();
    }
}
