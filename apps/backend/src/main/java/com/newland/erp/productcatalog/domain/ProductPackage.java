package com.newland.erp.productcatalog.domain;

public record ProductPackage(PackagingLevel level, int unitsPerPackage) {
    public ProductPackage {
        if (level == null) {
            throw new IllegalArgumentException("Packaging level is required.");
        }
        if (unitsPerPackage < 1) {
            throw new IllegalArgumentException("Units per package must be positive.");
        }
    }
}
