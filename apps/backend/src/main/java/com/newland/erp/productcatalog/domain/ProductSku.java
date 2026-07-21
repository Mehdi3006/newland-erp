package com.newland.erp.productcatalog.domain;

import java.util.Map;
import java.util.UUID;

public record ProductSku(UUID id, UUID productId, String skuCode, String gtin, String ean, String upc,
                         String barcode, String uomCode, Map<String, String> attributeValues) {
    public ProductSku {
        if (id == null || productId == null) {
            throw new IllegalArgumentException("SKU identifiers are required.");
        }
        skuCode = required("skuCode", skuCode).toUpperCase();
        gtin = optionalIdentifier(gtin);
        ean = optionalIdentifier(ean);
        upc = optionalIdentifier(upc);
        barcode = optionalIdentifier(barcode);
        uomCode = required("uomCode", uomCode).toUpperCase();
        attributeValues = attributeValues == null ? Map.of() : Map.copyOf(attributeValues);
    }

    private static String required(final String name, final String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("SKU " + name + " is required.");
        }
        return value.trim();
    }

    private static String optionalIdentifier(final String value) {
        return value == null || value.isBlank() ? null : value.trim().toUpperCase();
    }
}
