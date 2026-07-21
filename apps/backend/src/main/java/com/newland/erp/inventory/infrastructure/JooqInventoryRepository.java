package com.newland.erp.inventory.infrastructure;

import com.newland.erp.inventory.application.InventoryRepository;
import com.newland.erp.inventory.domain.InventoryItemReference;
import com.newland.erp.inventory.domain.InventoryLocation;
import com.newland.erp.inventory.domain.InventoryQuantity;
import com.newland.erp.inventory.domain.InventoryStatus;
import com.newland.erp.inventory.domain.MovementType;
import com.newland.erp.inventory.domain.Reservation;
import com.newland.erp.inventory.domain.StockBalance;
import com.newland.erp.inventory.domain.StockLedgerEntry;
import com.newland.erp.inventory.domain.StockMovementLine;
import com.newland.erp.inventory.domain.StockTransaction;
import com.newland.erp.inventory.domain.StockTransactionStatus;
import com.newland.erp.inventory.domain.TrackingPolicy;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public final class JooqInventoryRepository implements InventoryRepository {
    private final DSLContext dsl;

    public JooqInventoryRepository(final DSLContext dslContext) {
        this.dsl = dslContext;
    }

    @Override
    public boolean idempotencyKeyExists(final String idempotencyKey) {
        return dsl.fetchExists(transactionTable(), text("idempotency_key").eq(idempotencyKey))
                || dsl.fetchExists(table("inventory_reservation"), text("idempotency_key").eq(idempotencyKey));
    }

    @Override
    public StockTransaction insertTransaction(final StockTransaction transaction) {
        dsl.insertInto(transactionTable())
                .columns(id(), text("transaction_number"), text("idempotency_key"), text("movement_type"),
                        text("status"), uuid("reversed_transaction_id"), instant("posted_at"),
                        localDate("business_date"), text("actor"))
                .values(transaction.id(), transaction.transactionNumber(), transaction.idempotencyKey(),
                        transaction.movementType().name(), transaction.status().name(),
                        transaction.reversedTransactionId(), transaction.postedAt(), transaction.businessDate(),
                        transaction.actor())
                .execute();
        transaction.lines().forEach(line -> insertLine(transaction.id(), line));
        return transaction;
    }

    @Override
    public StockTransaction updateTransaction(final StockTransaction transaction) {
        dsl.update(transactionTable()).set(text("status"), transaction.status().name())
                .where(id().eq(transaction.id())).execute();
        return transaction;
    }

    @Override
    public Optional<StockTransaction> findTransaction(final UUID transactionId) {
        return dsl.selectFrom(transactionTable()).where(id().eq(transactionId)).fetchOptional(this::transaction);
    }

    @Override
    public List<StockTransaction> listTransactions() {
        return dsl.selectFrom(transactionTable()).orderBy(instant("posted_at")).fetch(this::transaction);
    }

    @Override
    public void appendLedgerEntries(final List<StockLedgerEntry> entries) {
        entries.forEach(entry -> dsl.insertInto(ledgerTable())
                .columns(id(), uuid("transaction_id"), uuid("line_id"), uuid("sku_id"), uuid("warehouse_id"),
                        uuid("zone_id"), uuid("bin_id"), decimal("quantity_delta"), text("uom_code"),
                        text("inventory_status"), text("lot_code"), text("serial_code"), localDate("expiry_date"),
                        instant("posted_at"))
                .values(entry.id(), entry.transactionId(), entry.lineId(), entry.skuId(),
                        entry.location().warehouseId(), entry.location().zoneId(), entry.location().binId(),
                        entry.quantityDelta().value(), entry.quantityDelta().uomCode(),
                        entry.inventoryStatus().name(), entry.lotCode(), entry.serialCode(), entry.expiryDate(),
                        entry.postedAt())
                .execute());
    }

    @Override
    public Optional<StockBalance> findBalanceForUpdate(final UUID skuId, final InventoryLocation location,
                                                       final InventoryStatus status) {
        return dsl.selectFrom(balanceTable())
                .where(uuid("sku_id").eq(skuId).and(uuid("warehouse_id").eq(location.warehouseId()))
                        .and(nullableUuid("zone_id", location.zoneId()))
                        .and(nullableUuid("bin_id", location.binId()))
                        .and(text("inventory_status").eq(status.name())))
                .forUpdate()
                .fetchOptional(this::balance);
    }

    @Override
    public StockBalance upsertBalance(final StockBalance balance) {
        dsl.insertInto(balanceTable())
                .columns(id(), uuid("sku_id"), uuid("warehouse_id"), uuid("zone_id"), uuid("bin_id"),
                        text("inventory_status"), decimal("on_hand_quantity"), decimal("reserved_quantity"),
                        decimal("in_transit_quantity"), decimal("damaged_quantity"),
                        decimal("quarantine_quantity"), text("uom_code"), longField("version"))
                .values(balance.id(), balance.skuId(), balance.location().warehouseId(), balance.location().zoneId(),
                        balance.location().binId(), balance.inventoryStatus().name(),
                        balance.onHandQuantity().value(), balance.reservedQuantity().value(),
                        balance.inTransitQuantity().value(), balance.damagedQuantity().value(),
                        balance.quarantineQuantity().value(), balance.onHandQuantity().uomCode(), balance.version())
                .onDuplicateKeyUpdate()
                .set(decimal("on_hand_quantity"), balance.onHandQuantity().value())
                .set(decimal("reserved_quantity"), balance.reservedQuantity().value())
                .set(decimal("in_transit_quantity"), balance.inTransitQuantity().value())
                .set(decimal("damaged_quantity"), balance.damagedQuantity().value())
                .set(decimal("quarantine_quantity"), balance.quarantineQuantity().value())
                .set(longField("version"), balance.version())
                .execute();
        return balance;
    }

    @Override
    public List<StockBalance> listBalances(final UUID skuId) {
        return dsl.selectFrom(balanceTable()).where(uuid("sku_id").eq(skuId)).fetch(this::balance);
    }

    @Override
    public Reservation insertReservation(final Reservation reservation) {
        dsl.insertInto(table("inventory_reservation"))
                .columns(id(), uuid("sku_id"), uuid("warehouse_id"), uuid("zone_id"), uuid("bin_id"),
                        decimal("quantity"), text("uom_code"), text("idempotency_key"), bool("released"),
                        instant("created_at"), instant("released_at"))
                .values(reservation.id(), reservation.skuId(), reservation.location().warehouseId(),
                        reservation.location().zoneId(), reservation.location().binId(),
                        reservation.quantity().value(), reservation.quantity().uomCode(),
                        reservation.idempotencyKey(), reservation.released(), reservation.createdAt(),
                        reservation.releasedAt())
                .execute();
        return reservation;
    }

    @Override
    public Reservation updateReservation(final Reservation reservation) {
        dsl.update(table("inventory_reservation")).set(bool("released"), reservation.released())
                .set(instant("released_at"), reservation.releasedAt()).where(id().eq(reservation.id())).execute();
        return reservation;
    }

    @Override
    public Optional<Reservation> findReservation(final UUID reservationId) {
        return dsl.selectFrom(table("inventory_reservation")).where(id().eq(reservationId))
                .fetchOptional(this::reservation);
    }

    private void insertLine(final UUID transactionId, final StockMovementLine line) {
        dsl.insertInto(table("inventory_stock_movement_line"))
                .columns(id(), uuid("transaction_id"), uuid("product_id"), uuid("sku_id"), text("sku_code"),
                        text("uom_code"), text("tracking_policy"), uuid("from_warehouse_id"),
                        uuid("from_zone_id"), uuid("from_bin_id"), uuid("to_warehouse_id"), uuid("to_zone_id"),
                        uuid("to_bin_id"), decimal("quantity"), text("inventory_status"), text("lot_code"),
                        text("serial_code"), localDate("expiry_date"))
                .values(line.id(), transactionId, line.item().productId(), line.item().skuId(), line.item().skuCode(),
                        line.item().uomCode(), line.item().trackingPolicy().name(),
                        locWarehouse(line.fromLocation()), locZone(line.fromLocation()), locBin(line.fromLocation()),
                        locWarehouse(line.toLocation()), locZone(line.toLocation()), locBin(line.toLocation()),
                        line.quantity().value(), line.inventoryStatus().name(), line.lotCode(), line.serialCode(),
                        line.expiryDate())
                .execute();
    }

    private StockTransaction transaction(final Record record) {
        final UUID transactionId = record.get(id());
        return new StockTransaction(transactionId, record.get(text("transaction_number")),
                record.get(text("idempotency_key")), MovementType.valueOf(record.get(text("movement_type"))),
                StockTransactionStatus.valueOf(record.get(text("status"))),
                record.get(uuid("reversed_transaction_id")), lines(transactionId), instantValue(record, "posted_at"),
                record.get(localDate("business_date")), record.get(text("actor")));
    }

    private List<StockMovementLine> lines(final UUID transactionId) {
        return dsl.selectFrom(table("inventory_stock_movement_line"))
                .where(uuid("transaction_id").eq(transactionId)).fetch(record -> new StockMovementLine(
                        record.get(id()), new InventoryItemReference(record.get(uuid("product_id")),
                        record.get(uuid("sku_id")), record.get(text("sku_code")), record.get(text("uom_code")),
                        TrackingPolicy.valueOf(record.get(text("tracking_policy")))),
                        location(record, "from"), location(record, "to"),
                        new InventoryQuantity(record.get(decimal("quantity")), record.get(text("uom_code"))),
                        InventoryStatus.valueOf(record.get(text("inventory_status"))), record.get(text("lot_code")),
                        record.get(text("serial_code")), record.get(localDate("expiry_date"))));
    }

    private StockBalance balance(final Record record) {
        final String uom = record.get(text("uom_code"));
        return new StockBalance(record.get(id()), record.get(uuid("sku_id")),
                new InventoryLocation(record.get(uuid("warehouse_id")), record.get(uuid("zone_id")),
                        record.get(uuid("bin_id"))), InventoryStatus.valueOf(record.get(text("inventory_status"))),
                new InventoryQuantity(record.get(decimal("on_hand_quantity")), uom),
                new InventoryQuantity(record.get(decimal("reserved_quantity")), uom),
                new InventoryQuantity(record.get(decimal("in_transit_quantity")), uom),
                new InventoryQuantity(record.get(decimal("damaged_quantity")), uom),
                new InventoryQuantity(record.get(decimal("quarantine_quantity")), uom),
                record.get(longField("version")));
    }

    private Reservation reservation(final Record record) {
        return new Reservation(record.get(id()), record.get(uuid("sku_id")),
                new InventoryLocation(record.get(uuid("warehouse_id")), record.get(uuid("zone_id")),
                        record.get(uuid("bin_id"))),
                new InventoryQuantity(record.get(decimal("quantity")), record.get(text("uom_code"))),
                record.get(text("idempotency_key")), Boolean.TRUE.equals(record.get(bool("released"))),
                instantValue(record, "created_at"), instantValue(record, "released_at"));
    }

    private static InventoryLocation location(final Record record, final String prefix) {
        final UUID warehouseId = record.get(uuid(prefix + "_warehouse_id"));
        return warehouseId == null ? null : new InventoryLocation(warehouseId, record.get(uuid(prefix + "_zone_id")),
                record.get(uuid(prefix + "_bin_id")));
    }

    private static UUID locWarehouse(final InventoryLocation location) {
        return location == null ? null : location.warehouseId();
    }

    private static UUID locZone(final InventoryLocation location) {
        return location == null ? null : location.zoneId();
    }

    private static UUID locBin(final InventoryLocation location) {
        return location == null ? null : location.binId();
    }

    private static org.jooq.Condition nullableUuid(final String name, final UUID value) {
        return value == null ? uuid(name).isNull() : uuid(name).eq(value);
    }

    private static Table<Record> transactionTable() {
        return table("inventory_stock_transaction");
    }

    private static Table<Record> ledgerTable() {
        return table("inventory_stock_ledger_entry");
    }

    private static Table<Record> balanceTable() {
        return table("inventory_stock_balance");
    }

    private static Table<Record> table(final String name) {
        return DSL.table(DSL.name(name));
    }

    private static Field<UUID> id() {
        return uuid("id");
    }

    private static Field<UUID> uuid(final String name) {
        return DSL.field(DSL.name(name), UUID.class);
    }

    private static Field<String> text(final String name) {
        return DSL.field(DSL.name(name), String.class);
    }

    private static Field<Boolean> bool(final String name) {
        return DSL.field(DSL.name(name), Boolean.class);
    }

    private static Field<BigDecimal> decimal(final String name) {
        return DSL.field(DSL.name(name), BigDecimal.class);
    }

    private static Field<Long> longField(final String name) {
        return DSL.field(DSL.name(name), Long.class);
    }

    private static Field<Instant> instant(final String name) {
        return DSL.field(DSL.name(name), Instant.class);
    }

    private static Field<LocalDate> localDate(final String name) {
        return DSL.field(DSL.name(name), LocalDate.class);
    }

    private static Instant instantValue(final Record record, final String name) {
        final Object value = record.get(DSL.field(DSL.name(name)));
        if (value == null) {
            return null;
        }
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime.toInstant();
        }
        throw new IllegalStateException("Unsupported timestamp value for " + name + ".");
    }
}
