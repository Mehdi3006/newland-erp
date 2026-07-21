package com.newland.erp.inventory.application;

import com.newland.erp.inventory.domain.InventoryConflictException;
import com.newland.erp.inventory.domain.InventoryItemReference;
import com.newland.erp.inventory.domain.InventoryLocation;
import com.newland.erp.inventory.domain.InventoryQuantity;
import com.newland.erp.inventory.domain.InventoryStatus;
import com.newland.erp.inventory.domain.MovementType;
import com.newland.erp.inventory.domain.StockTransactionStatus;
import com.newland.erp.inventory.domain.TrackingPolicy;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

final class InventoryServiceTest {
    private static final LocalDate DATE = LocalDate.parse("2026-07-21");
    private final InMemoryInventoryRepository repository = new InMemoryInventoryRepository();
    private final InventoryService service = service(false);
    private final InventoryItemReference item = item(TrackingPolicy.NONE);
    private final InventoryLocation warehouseA = location();
    private final InventoryLocation warehouseB = location();

    @Test
    void openingBalanceAndReceiptIncreaseStock() {
        service.post(post(MovementType.OPENING_BALANCE, "open-1", null, warehouseA, qty("10"), item,
                InventoryStatus.AVAILABLE, null, null));
        service.post(post(MovementType.GOODS_RECEIPT, "receipt-1", null, warehouseA, qty("5"), item,
                InventoryStatus.AVAILABLE, null, null));

        assertThat(balance().onHandQuantity().value()).isEqualByComparingTo("15");
    }

    @Test
    void issueDecreasesStockAndRejectsNegativeStock() {
        service.post(post(MovementType.OPENING_BALANCE, "open-1", null, warehouseA, qty("10"), item,
                InventoryStatus.AVAILABLE, null, null));
        service.post(post(MovementType.GOODS_ISSUE, "issue-1", warehouseA, null, qty("4"), item,
                InventoryStatus.AVAILABLE, null, null));

        assertThat(balance().onHandQuantity().value()).isEqualByComparingTo("6");
        assertThatThrownBy(() -> service.post(post(MovementType.GOODS_ISSUE, "issue-2", warehouseA, null,
                qty("7"), item, InventoryStatus.AVAILABLE, null, null)))
                .isInstanceOf(InventoryConflictException.class);
    }

    @Test
    void transferPreservesTotalStock() {
        service.post(post(MovementType.OPENING_BALANCE, "open-1", null, warehouseA, qty("10"), item,
                InventoryStatus.AVAILABLE, null, null));
        service.post(post(MovementType.WAREHOUSE_TRANSFER, "transfer-1", warehouseA, warehouseB, qty("3"), item,
                InventoryStatus.AVAILABLE, null, null));

        final BigDecimal total = repository.balances.stream().map(balance -> balance.onHandQuantity().value())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(total).isEqualByComparingTo("10");
    }

    @Test
    void reservationReducesAvailableAndReleaseRestoresAvailableWithoutChangingOnHand() {
        service.post(post(MovementType.OPENING_BALANCE, "open-1", null, warehouseA, qty("10"), item,
                InventoryStatus.AVAILABLE, null, null));
        final var reservation = service.reserve(new InventoryCommands.Reserve(item, warehouseA, qty("4"),
                "reserve-1", "architect"));
        assertThat(balance().onHandQuantity().value()).isEqualByComparingTo("10");
        assertThat(balance().availableQuantity().value()).isEqualByComparingTo("6");

        service.release(new InventoryCommands.Release(reservation.id(), "release-1", "architect"));

        assertThat(balance().availableQuantity().value()).isEqualByComparingTo("10");
    }

    @Test
    void duplicateIdempotencyAndPostedImmutabilityAndReversalAreEnforced() {
        final var posted = service.post(post(MovementType.OPENING_BALANCE, "open-1", null, warehouseA, qty("10"),
                item, InventoryStatus.AVAILABLE, null, null));
        assertThatThrownBy(() -> service.post(post(MovementType.GOODS_RECEIPT, "open-1", null, warehouseA,
                qty("1"), item, InventoryStatus.AVAILABLE, null, null)))
                .isInstanceOf(InventoryConflictException.class);

        final var reversal = service.reverse(new InventoryCommands.Reverse(posted.id(), "rev-1", "architect"));

        assertThat(repository.findTransaction(posted.id()).orElseThrow().status())
                .isEqualTo(StockTransactionStatus.REVERSED);
        assertThat(reversal.movementType()).isEqualTo(MovementType.REVERSAL);
        assertThat(balance().onHandQuantity().value()).isEqualByComparingTo("0");
    }

    @Test
    void lotAndSerialControlledPostingRulesAreEnforced() {
        final InventoryItemReference lotItem = item(TrackingPolicy.LOT);
        service.post(post(MovementType.GOODS_RECEIPT, "lot-1", null, warehouseA, qty("2"), lotItem,
                InventoryStatus.AVAILABLE, "LOT-1", null, LocalDate.parse("2027-01-01")));
        service.post(post(MovementType.GOODS_ISSUE, "lot-2", warehouseA, null, qty("1"), lotItem,
                InventoryStatus.AVAILABLE, "LOT-1", null, LocalDate.parse("2027-01-01")));
        service.post(post(MovementType.GOODS_RECEIPT, "serial-1", null, warehouseA, qty("1"),
                item(TrackingPolicy.SERIAL), InventoryStatus.AVAILABLE, null, "SN-1", null));

        assertThatThrownBy(() -> service.post(post(MovementType.GOODS_RECEIPT, "serial-2", null, warehouseA,
                qty("2"), item(TrackingPolicy.SERIAL), InventoryStatus.AVAILABLE, null, "SN-2")))
                .isInstanceOf(InventoryConflictException.class);
    }

    @Test
    void expiredStockCannotBeIssued() {
        service.post(post(MovementType.OPENING_BALANCE, "open-expiry", null, warehouseA, qty("2"), item,
                InventoryStatus.AVAILABLE, "LOT-OLD", null, LocalDate.parse("2026-01-01")));

        assertThatThrownBy(() -> service.post(post(MovementType.GOODS_ISSUE, "issue-expiry", warehouseA, null,
                qty("1"), item, InventoryStatus.AVAILABLE, "LOT-OLD", null, LocalDate.parse("2026-01-01"))))
                .isInstanceOf(InventoryConflictException.class);
    }

    @Test
    void quarantinedOrDamagedStockIsNotIssuedAsNormalAvailableStock() {
        service.post(post(MovementType.GOODS_RECEIPT, "quarantine-1", null, warehouseA, qty("3"), item,
                InventoryStatus.QUARANTINE, null, null));

        assertThatThrownBy(() -> service.post(post(MovementType.GOODS_ISSUE, "issue-1", warehouseA, null, qty("1"),
                item, InventoryStatus.QUARANTINE, null, null)))
                .isInstanceOf(InventoryConflictException.class);
    }

    @Test
    void concurrentPostingProtectionUsesLockedBalanceVersion() {
        service.post(post(MovementType.OPENING_BALANCE, "open-1", null, warehouseA, qty("1"), item,
                InventoryStatus.AVAILABLE, null, null));
        final long version = balance().version();

        service.post(post(MovementType.GOODS_RECEIPT, "receipt-1", null, warehouseA, qty("1"), item,
                InventoryStatus.AVAILABLE, null, null));

        assertThat(balance().version()).isGreaterThan(version);
    }

    private StockBalanceView balance() {
        final var balance = repository.findBalanceForUpdate(item.skuId(), warehouseA, InventoryStatus.AVAILABLE)
                .orElseThrow();
        return new StockBalanceView(balance.onHandQuantity(), balance.availableQuantity(), balance.version());
    }

    private InventoryService service(final boolean negativeStockAllowed) {
        final AtomicLong sequence = new AtomicLong();
        return new InventoryService(repository, ignored -> {
        }, ignored -> {
        }, () -> negativeStockAllowed, prefix -> prefix + "-" + sequence.incrementAndGet(), (actor, action, id) -> {
        }, (eventType, aggregateId) -> {
        }, (transactionId, attachmentId) -> {
        }, (actor, capability) -> {
        }, Clock.fixed(Instant.parse("2026-07-21T00:00:00Z"), ZoneOffset.UTC));
    }

    private static InventoryCommands.PostTransaction post(final MovementType type, final String key,
                                                          final InventoryLocation from,
                                                          final InventoryLocation to,
                                                          final InventoryQuantity quantity,
                                                          final InventoryItemReference item,
                                                          final InventoryStatus status,
                                                          final String lot,
                                                          final String serial) {
        return post(type, key, from, to, quantity, item, status, lot, serial, null);
    }

    private static InventoryCommands.PostTransaction post(final MovementType type, final String key,
                                                          final InventoryLocation from,
                                                          final InventoryLocation to,
                                                          final InventoryQuantity quantity,
                                                          final InventoryItemReference item,
                                                          final InventoryStatus status,
                                                          final String lot,
                                                          final String serial,
                                                          final LocalDate expiryDate) {
        return new InventoryCommands.PostTransaction(type, key, DATE, List.of(new InventoryCommands.PostLine(item,
                from, to, quantity, status, lot, serial, expiryDate)), List.of(UUID.randomUUID()), "architect");
    }

    private static InventoryItemReference item(final TrackingPolicy policy) {
        return new InventoryItemReference(UUID.randomUUID(), UUID.randomUUID(), "SKU-1", "EA", policy);
    }

    private static InventoryLocation location() {
        return new InventoryLocation(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
    }

    private static InventoryQuantity qty(final String value) {
        return new InventoryQuantity(new BigDecimal(value), "EA");
    }

    private record StockBalanceView(InventoryQuantity onHandQuantity, InventoryQuantity availableQuantity,
                                    long version) {
    }
}
