package com.newland.erp.procurement.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

final class ProcurementDomainTest {
    @Test
    void purchaseOrderRemainingQuantityIsConsistentAcrossDeliveryAndCancellation() {
        final PurchaseOrder order = order();
        final UUID lineId = order.lines().getFirst().id();

        final PurchaseOrder partiallyReceived = order.approve().receive(lineId, qty("4"));
        final PurchaseOrder cancelled = partiallyReceived.cancel();

        assertThat(partiallyReceived.lines().getFirst().remainingQuantity().value()).isEqualByComparingTo("6");
        assertThat(cancelled.lines().getFirst().receivedQuantity().value()).isEqualByComparingTo("4");
        assertThat(cancelled.lines().getFirst().cancelledQuantity().value()).isEqualByComparingTo("6");
        assertThat(cancelled.lines().getFirst().remainingQuantity().value()).isEqualByComparingTo("0");
    }

    @Test
    void approvedPurchaseOrderRequiresControlledAmendment() {
        final PurchaseOrder approved = order().approve();

        final PurchaseOrder amended = approved.amend(List.of(line("12")));

        assertThat(amended.revision()).isEqualTo(1);
        assertThat(amended.status()).isEqualTo(PurchaseOrderStatus.DRAFT);
    }

    @Test
    void receivedAndCancelledCannotExceedOrderedQuantity() {
        assertThatThrownBy(() -> new PurchaseOrder.PurchaseOrderLine(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "SKU-1", qty("5"), qty("3"), qty("3"), null))
                .isInstanceOf(ProcurementConflictException.class);
    }

    @Test
    void approvedRequisitionIsImmutableExceptRevisionFlow() {
        final PurchaseRequisition requisition = new PurchaseRequisition(UUID.randomUUID(), "PR-1", "idem-1",
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), RequisitionStatus.SUBMITTED, 0,
                List.of(procurementLine()), Instant.now(), "architect").approve();

        assertThatThrownBy(requisition::approve).isInstanceOf(ProcurementConflictException.class);
    }

    static PurchaseOrder order() {
        return new PurchaseOrder(UUID.randomUUID(), "PO-1", "idem-po", UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), PurchaseOrderStatus.DRAFT,
                0, List.of(line("10")), LocalDate.parse("2026-08-01"), Instant.now(), "architect");
    }

    static PurchaseOrder.PurchaseOrderLine line(final String quantity) {
        final ProcurementQuantity zero = qty("0");
        return new PurchaseOrder.PurchaseOrderLine(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "SKU-1",
                qty(quantity), zero, zero, UUID.randomUUID());
    }

    static ProcurementLine procurementLine() {
        return new ProcurementLine(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "SKU-1", qty("5"),
                BigDecimal.TEN, UUID.randomUUID());
    }

    static ProcurementQuantity qty(final String value) {
        return new ProcurementQuantity(new BigDecimal(value), "EA");
    }
}
