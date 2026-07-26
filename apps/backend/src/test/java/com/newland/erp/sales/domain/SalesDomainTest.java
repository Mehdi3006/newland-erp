package com.newland.erp.sales.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

final class SalesDomainTest {
    @Test
    void customerStatusTransitionsAreControlled() {
        final Customer blocked = customer().transitionTo(CustomerStatus.BLOCKED);

        assertThatThrownBy(() -> blocked.transitionTo(CustomerStatus.ACTIVE))
                .isInstanceOf(SalesConflictException.class);
    }

    @Test
    void duplicateCustomerProductReferencesAreRejected() {
        final UUID skuId = UUID.randomUUID();

        assertThatThrownBy(() -> new Customer(UUID.randomUUID(), "idem", "CUS-1", "Customer",
                CustomerStatus.ACTIVE, List.of(), List.of(), List.of(),
                List.of(reference(skuId), reference(skuId)), Instant.now()))
                .isInstanceOf(SalesConflictException.class);
    }

    @Test
    void salesOrderQuantitiesRemainConsistentAcrossReservationDeliveryAndCancellation() {
        final SalesOrder approved = order().approve();
        final UUID lineId = approved.lines().getFirst().id();
        final SalesOrder reserved = approved.reserve(lineId, qty("2"));
        final SalesOrder delivered = reserved.deliver(
                lineId, qty("3"), Instant.parse("2026-01-03T00:00:00Z"));
        final SalesOrder deliveredAgain = delivered.deliver(
                lineId, qty("1"), Instant.parse("2026-01-04T00:00:00Z"));
        final SalesOrder cancelled = deliveredAgain.cancel();

        assertThat(cancelled.lines().getFirst().reservedQuantity().value()).isEqualByComparingTo("2");
        assertThat(cancelled.lines().getFirst().deliveredQuantity().value()).isEqualByComparingTo("4");
        assertThat(cancelled.lines().getFirst().cancelledQuantity().value()).isEqualByComparingTo("4");
        assertThat(cancelled.lines().getFirst().remainingQuantity().value()).isEqualByComparingTo("0");
        assertThat(cancelled.lines().getFirst().deliveredAt())
                .isEqualTo(Instant.parse("2026-01-03T00:00:00Z"));
    }

    @Test
    void fullReservationDoesNotMarkOrderDelivered() {
        final SalesOrder approved = order().approve();

        final SalesOrder reserved = approved.reserve(approved.lines().getFirst().id(), qty("10"));

        assertThat(reserved.status()).isEqualTo(SalesOrderStatus.PARTIALLY_RESERVED);
    }

    @Test
    void approvedQuotationIsImmutableExceptControlledRevision() {
        final SalesQuotation approved = quotation().submit().approve();

        assertThatThrownBy(approved::approve).isInstanceOf(SalesConflictException.class);
        assertThat(approved.revise(List.of(line("8"))).revision()).isEqualTo(1);
    }

    @Test
    void submittedQuotationCanBeRejectedOnce() {
        final SalesQuotation rejected = quotation().submit().reject();

        assertThat(rejected.status()).isEqualTo(SalesQuotationStatus.REJECTED);
        assertThatThrownBy(rejected::reject).isInstanceOf(SalesConflictException.class);
    }

    @Test
    void expiredQuotationCannotConvertNormally() {
        final SalesQuotation expired = quotation().submit().approve().expire(LocalDate.parse("2026-08-01"));

        assertThatThrownBy(expired::converted).isInstanceOf(SalesConflictException.class);
    }

    static Customer customer() {
        return new Customer(UUID.randomUUID(), "idem-customer", "CUS-1", "Customer", CustomerStatus.ACTIVE,
                List.of(), List.of(), List.of(new Customer.CustomerCreditProfile(UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), BigDecimal.TEN, false)), List.of(), Instant.now());
    }

    static SalesQuotation quotation() {
        return new SalesQuotation(UUID.randomUUID(), "SQ-1", "idem-q", UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), SalesQuotationStatus.DRAFT, 0, List.of(line("10")),
                0, LocalDate.parse("2026-07-01"), Instant.now(), "architect");
    }

    static SalesOrder order() {
        return new SalesOrder(UUID.randomUUID(), "SO-1", "idem-o", UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                SalesOrderStatus.DRAFT, 0, List.of(orderLine("10")), 0, LocalDate.parse("2026-08-01"),
                Instant.now(), "architect");
    }

    static SalesLine line(final String quantity) {
        return new SalesLine(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "SKU-1", qty(quantity),
                BigDecimal.TEN, UUID.randomUUID());
    }

    static SalesOrder.SalesOrderLine orderLine(final String quantity) {
        final SalesQuantity zero = qty("0");
        return new SalesOrder.SalesOrderLine(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "SKU-1",
                qty(quantity), zero, zero, zero, UUID.randomUUID(), null);
    }

    static SalesQuantity qty(final String value) {
        return new SalesQuantity(new BigDecimal(value), "EA");
    }

    private static Customer.CustomerProductReference reference(final UUID skuId) {
        return new Customer.CustomerProductReference(UUID.randomUUID(), UUID.randomUUID(), skuId, "CUS-SKU");
    }
}
