package com.newland.erp.inventory.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

final class InventoryDomainTest {
    @Test
    void serialControlledLineRequiresQuantityOne() {
        final InventoryItemReference item = item(TrackingPolicy.SERIAL);

        assertThatThrownBy(() -> new StockMovementLine(UUID.randomUUID(), item, null, location(),
                quantity("2"), InventoryStatus.AVAILABLE, null, "SN-1", null))
                .isInstanceOf(InventoryConflictException.class);
    }

    @Test
    void lotDetectsExpiredStock() {
        final Lot lot = new Lot(UUID.randomUUID(), UUID.randomUUID(), "LOT-1", LocalDate.parse("2026-01-01"));

        assertThat(lot.isExpired(LocalDate.parse("2026-07-21"))).isTrue();
    }

    @Test
    void stockBalanceComputesAvailableQuantity() {
        final StockBalance balance = new StockBalance(UUID.randomUUID(), UUID.randomUUID(), location(),
                InventoryStatus.AVAILABLE, quantity("10"), quantity("3"), quantity("0"), quantity("2"),
                quantity("1"), 0);

        assertThat(balance.availableQuantity().value()).isEqualByComparingTo("4");
    }

    @Test
    void postedTransactionRequiresLinesAndIsReversedByCompensation() {
        final StockMovementLine line = new StockMovementLine(UUID.randomUUID(), item(TrackingPolicy.NONE), null,
                location(), quantity("5"), InventoryStatus.AVAILABLE, null, null, null);
        final StockTransaction transaction = new StockTransaction(UUID.randomUUID(), "INV-1", "idem-1",
                MovementType.GOODS_RECEIPT, StockTransactionStatus.POSTED, null, List.of(line), Instant.now(),
                LocalDate.parse("2026-07-21"), "architect");

        final StockTransaction reversal = transaction.reversed("REV-1", "idem-2", Instant.now(), "architect");

        assertThat(reversal.movementType()).isEqualTo(MovementType.REVERSAL);
        assertThat(reversal.lines()).extracting(StockMovementLine::id).doesNotContain(line.id());
    }

    static InventoryItemReference item(final TrackingPolicy trackingPolicy) {
        return new InventoryItemReference(UUID.randomUUID(), UUID.randomUUID(), "SKU-1", "EA", trackingPolicy);
    }

    static InventoryLocation location() {
        return new InventoryLocation(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
    }

    static InventoryQuantity quantity(final String value) {
        return new InventoryQuantity(new BigDecimal(value), "EA");
    }
}
