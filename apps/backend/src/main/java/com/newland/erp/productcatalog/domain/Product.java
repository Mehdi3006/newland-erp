package com.newland.erp.productcatalog.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record Product(UUID id, String productCode, ProductStatus status, UUID categoryId, UUID brandId,
                      UUID familyId, List<ProductSku> skus, List<ProductPackage> packagingLevels,
                      ProductMeasurement length, ProductMeasurement width, ProductMeasurement height,
                      ProductMeasurement weight, List<ProductMedia> media, List<ProductContent> content,
                      List<String> tags, Map<String, String> searchMetadata, Map<String, String> warrantyMetadata,
                      long version, Instant createdAt, Instant updatedAt) {
    public Product {
        if (id == null) {
            throw new IllegalArgumentException("Product id is required.");
        }
        productCode = required("productCode", productCode).toUpperCase();
        if (status == null) {
            throw new IllegalArgumentException("Product status is required.");
        }
        skus = skus == null ? List.of() : List.copyOf(skus);
        packagingLevels = packagingLevels == null ? List.of() : List.copyOf(packagingLevels);
        media = media == null ? List.of() : List.copyOf(media);
        content = content == null ? List.of() : List.copyOf(content);
        tags = tags == null ? List.of() : List.copyOf(tags);
        searchMetadata = searchMetadata == null ? Map.of() : Map.copyOf(searchMetadata);
        warrantyMetadata = warrantyMetadata == null ? Map.of() : Map.copyOf(warrantyMetadata);
        if (version < 0) {
            throw new IllegalArgumentException("Product version cannot be negative.");
        }
        if (createdAt == null || updatedAt == null) {
            throw new IllegalArgumentException("Product timestamps are required.");
        }
    }

    public Product withStatus(final ProductStatus nextStatus, final Instant changedAt) {
        return new Product(id, productCode, nextStatus, categoryId, brandId, familyId, skus, packagingLevels,
                length, width, height, weight, media, content, tags, searchMetadata, warrantyMetadata,
                version + 1, createdAt, changedAt);
    }

    private static String required(final String name, final String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Product " + name + " is required.");
        }
        return value.trim();
    }
}
