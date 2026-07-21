package com.newland.erp.procurement.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record Supplier(UUID id, String idempotencyKey, String supplierCode, String name, SupplierStatus status,
                       List<SupplierContact> contacts, List<SupplierAddress> addresses,
                       List<SupplierProductReference> productReferences, Instant createdAt) {
    public Supplier {
        if (id == null || status == null || createdAt == null) {
            throw new IllegalArgumentException("Supplier identifiers, status and creation time are required.");
        }
        idempotencyKey = required("idempotencyKey", idempotencyKey);
        supplierCode = required("supplierCode", supplierCode).toUpperCase();
        name = required("name", name);
        contacts = contacts == null ? List.of() : List.copyOf(contacts);
        addresses = addresses == null ? List.of() : List.copyOf(addresses);
        productReferences = productReferences == null ? List.of() : List.copyOf(productReferences);
        final long distinctSkuReferences = productReferences.stream().map(SupplierProductReference::skuId)
                .distinct().count();
        if (distinctSkuReferences != productReferences.size()) {
            throw new ProcurementConflictException("Duplicate supplier product reference.");
        }
    }

    public record SupplierContact(UUID id, String name, String email, String phone) {
        public SupplierContact {
            if (id == null) {
                throw new IllegalArgumentException("Supplier contact id is required.");
            }
            name = required("contact name", name);
        }
    }

    public record SupplierAddress(UUID id, UUID countryId, UUID provinceId, UUID cityId, String addressLine) {
        public SupplierAddress {
            if (id == null || countryId == null) {
                throw new IllegalArgumentException("Supplier address id and country are required.");
            }
            addressLine = required("addressLine", addressLine);
        }
    }

    public record SupplierProductReference(UUID id, UUID productId, UUID skuId, String supplierSku,
                                           int leadTimeDays, ProcurementQuantity minimumOrderQuantity,
                                           String packagingInformation) {
        public SupplierProductReference {
            if (id == null || productId == null || skuId == null || minimumOrderQuantity == null) {
                throw new IllegalArgumentException("Supplier product reference identifiers are required.");
            }
            supplierSku = required("supplierSku", supplierSku).toUpperCase();
            if (leadTimeDays < 0) {
                throw new IllegalArgumentException("Supplier lead time cannot be negative.");
            }
            packagingInformation = packagingInformation == null ? "" : packagingInformation.trim();
        }
    }

    private static String required(final String name, final String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Supplier " + name + " is required.");
        }
        return value.trim();
    }
}
