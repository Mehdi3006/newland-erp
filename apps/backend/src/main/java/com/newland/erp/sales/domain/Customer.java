package com.newland.erp.sales.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record Customer(UUID id, String idempotencyKey, String customerCode, String name, CustomerStatus status,
                       List<CustomerContact> contacts, List<CustomerAddress> addresses,
                       List<CustomerCreditProfile> creditProfiles,
                       List<CustomerProductReference> productReferences, Instant createdAt) {
    public Customer {
        if (id == null || status == null || createdAt == null) {
            throw new IllegalArgumentException("Customer identifiers, status and creation time are required.");
        }
        idempotencyKey = SalesLine.required("idempotencyKey", idempotencyKey);
        customerCode = SalesLine.required("customerCode", customerCode).toUpperCase();
        name = SalesLine.required("customer name", name);
        contacts = contacts == null ? List.of() : List.copyOf(contacts);
        addresses = addresses == null ? List.of() : List.copyOf(addresses);
        creditProfiles = creditProfiles == null ? List.of() : List.copyOf(creditProfiles);
        productReferences = productReferences == null ? List.of() : List.copyOf(productReferences);
        if (productReferences.stream().map(CustomerProductReference::skuId).distinct().count()
                != productReferences.size()) {
            throw new SalesConflictException("Duplicate customer product reference.");
        }
    }

    public Customer transitionTo(final CustomerStatus nextStatus) {
        if (nextStatus == null) {
            throw new IllegalArgumentException("Next customer status is required.");
        }
        if (status == CustomerStatus.BLOCKED && nextStatus == CustomerStatus.ACTIVE) {
            throw new SalesConflictException("Blocked customers require controlled reactivation.");
        }
        return new Customer(id, idempotencyKey, customerCode, name, nextStatus, contacts, addresses,
                creditProfiles, productReferences, createdAt);
    }

    public record CustomerContact(UUID id, String name, String email, String phone) {
        public CustomerContact {
            if (id == null) {
                throw new IllegalArgumentException("Customer contact id is required.");
            }
            name = SalesLine.required("contact name", name);
        }
    }

    public record CustomerAddress(UUID id, UUID countryId, UUID provinceId, UUID cityId, String addressLine) {
        public CustomerAddress {
            if (id == null || countryId == null) {
                throw new IllegalArgumentException("Customer address id and country are required.");
            }
            addressLine = SalesLine.required("addressLine", addressLine);
        }
    }

    public record CustomerCreditProfile(UUID id, UUID companyId, UUID currencyId, BigDecimal creditLimit,
                                        boolean creditHold) {
        public CustomerCreditProfile {
            if (id == null || companyId == null || currencyId == null || creditLimit == null
                    || creditLimit.signum() < 0) {
                throw new IllegalArgumentException("Customer credit profile company, currency and limit are required.");
            }
        }
    }

    public record CustomerProductReference(UUID id, UUID productId, UUID skuId, String customerSku) {
        public CustomerProductReference {
            if (id == null || productId == null || skuId == null) {
                throw new IllegalArgumentException("Customer product reference identifiers are required.");
            }
            customerSku = SalesLine.required("customerSku", customerSku).toUpperCase();
        }
    }
}
