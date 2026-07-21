package com.newland.erp.inventory.application;

import com.newland.erp.inventory.domain.InventoryConflictException;
import com.newland.erp.inventory.domain.InventoryLocation;
import com.newland.erp.inventory.domain.InventoryNotFoundException;
import com.newland.erp.inventory.domain.InventoryQuantity;
import com.newland.erp.inventory.domain.InventoryStatus;
import com.newland.erp.inventory.domain.MovementType;
import com.newland.erp.inventory.domain.Reservation;
import com.newland.erp.inventory.domain.StockBalance;
import com.newland.erp.inventory.domain.StockLedgerEntry;
import com.newland.erp.inventory.domain.StockMovementLine;
import com.newland.erp.inventory.domain.StockTransaction;
import com.newland.erp.inventory.domain.StockTransactionStatus;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public final class InventoryService {
    private final InventoryRepository repository;
    private final InventoryPorts.ProductCatalogPort productCatalog;
    private final InventoryPorts.WarehouseReferencePort warehouseReferences;
    private final InventoryPorts.PlatformConfigurationPort configuration;
    private final InventoryPorts.NumberSeriesPort numberSeries;
    private final InventoryPorts.AuditPort audit;
    private final InventoryPorts.DomainEventPort events;
    private final InventoryPorts.AttachmentPort attachments;
    private final InventoryPorts.AuthorizationPort authorization;
    private final Clock clock;

    public InventoryService(final InventoryRepository inventoryRepository,
                            final InventoryPorts.ProductCatalogPort productCatalogPort,
                            final InventoryPorts.WarehouseReferencePort warehouseReferencePort,
                            final InventoryPorts.PlatformConfigurationPort configurationPort,
                            final InventoryPorts.NumberSeriesPort numberSeriesPort,
                            final InventoryPorts.AuditPort auditPort,
                            final InventoryPorts.DomainEventPort eventPort,
                            final InventoryPorts.AttachmentPort attachmentPort,
                            final InventoryPorts.AuthorizationPort authorizationPort,
                            final Clock systemClock) {
        this.repository = inventoryRepository;
        this.productCatalog = productCatalogPort;
        this.warehouseReferences = warehouseReferencePort;
        this.configuration = configurationPort;
        this.numberSeries = numberSeriesPort;
        this.audit = auditPort;
        this.events = eventPort;
        this.attachments = attachmentPort;
        this.authorization = authorizationPort;
        this.clock = systemClock;
    }

    @Transactional
    public StockTransaction post(final InventoryCommands.PostTransaction command) {
        authorization.requirePermission(command.actor(), "inventory.transaction.post");
        assertIdempotent(command.idempotencyKey());
        final Instant now = Instant.now(clock);
        final StockTransaction transaction = new StockTransaction(UUID.randomUUID(),
                numberSeries.nextTransactionNumber("INV"), command.idempotencyKey(), command.movementType(),
                StockTransactionStatus.POSTED, null, lines(command), now, command.businessDate(), command.actor());
        final List<StockLedgerEntry> ledger = ledgerFor(transaction);
        applyLedger(ledger);
        repository.insertTransaction(transaction);
        repository.appendLedgerEntries(ledger);
        command.attachmentIds().forEach(attachmentId -> attachments.attach(transaction.id(), attachmentId));
        audit.record(command.actor(), "INVENTORY_TRANSACTION_POSTED", transaction.id());
        events.publish("InventoryTransactionPosted", transaction.id());
        return transaction;
    }

    @Transactional
    public Reservation reserve(final InventoryCommands.Reserve command) {
        authorization.requirePermission(command.actor(), "inventory.reservation.create");
        assertIdempotent(command.idempotencyKey());
        productCatalog.requireSku(command.item());
        warehouseReferences.requireLocation(command.location());
        final StockBalance balance = balance(command.item().skuId(), command.location(), InventoryStatus.AVAILABLE,
                command.quantity().uomCode());
        final InventoryQuantity availableAfter = balance.availableQuantity().subtract(command.quantity());
        if (availableAfter.isNegative()) {
            throw new InventoryConflictException("Reservation exceeds available quantity.");
        }
        final Reservation reservation = repository.insertReservation(new Reservation(UUID.randomUUID(),
                command.item().skuId(), command.location(), command.quantity(), command.idempotencyKey(), false,
                Instant.now(clock), null));
        repository.upsertBalance(new StockBalance(balance.id(), balance.skuId(), balance.location(),
                balance.inventoryStatus(), balance.onHandQuantity(), balance.reservedQuantity().add(command.quantity()),
                balance.inTransitQuantity(), balance.damagedQuantity(), balance.quarantineQuantity(),
                balance.version() + 1));
        audit.record(command.actor(), "INVENTORY_RESERVED", reservation.id());
        return reservation;
    }

    @Transactional
    public Reservation release(final InventoryCommands.Release command) {
        authorization.requirePermission(command.actor(), "inventory.reservation.release");
        assertIdempotent(command.idempotencyKey());
        final Reservation reservation = repository.findReservation(command.reservationId())
                .orElseThrow(() -> new InventoryNotFoundException("Reservation not found: "
                        + command.reservationId()));
        if (reservation.released()) {
            throw new InventoryConflictException("Reservation already released.");
        }
        final StockBalance balance = balance(reservation.skuId(), reservation.location(), InventoryStatus.AVAILABLE,
                reservation.quantity().uomCode());
        repository.upsertBalance(new StockBalance(balance.id(), balance.skuId(), balance.location(),
                balance.inventoryStatus(), balance.onHandQuantity(),
                balance.reservedQuantity().subtract(reservation.quantity()), balance.inTransitQuantity(),
                balance.damagedQuantity(), balance.quarantineQuantity(), balance.version() + 1));
        final Reservation released = new Reservation(reservation.id(), reservation.skuId(), reservation.location(),
                reservation.quantity(), reservation.idempotencyKey(), true, reservation.createdAt(),
                Instant.now(clock));
        repository.updateReservation(released);
        audit.record(command.actor(), "INVENTORY_RESERVATION_RELEASED", released.id());
        return released;
    }

    @Transactional
    public StockTransaction reverse(final InventoryCommands.Reverse command) {
        authorization.requirePermission(command.actor(), "inventory.transaction.reverse");
        assertIdempotent(command.idempotencyKey());
        final StockTransaction original = repository.findTransaction(command.transactionId())
                .orElseThrow(() -> new InventoryNotFoundException("Transaction not found: "
                        + command.transactionId()));
        if (original.status() != StockTransactionStatus.POSTED) {
            throw new InventoryConflictException("Only posted transactions can be reversed.");
        }
        final StockTransaction reversal = original.reversed(numberSeries.nextTransactionNumber("REV"),
                command.idempotencyKey(), Instant.now(clock), command.actor());
        final List<StockLedgerEntry> reversingLedger = ledgerFor(original).stream()
                .map(entry -> new StockLedgerEntry(UUID.randomUUID(), reversal.id(), entry.lineId(), entry.skuId(),
                        entry.location(), new InventoryQuantity(entry.quantityDelta().value().negate(),
                        entry.quantityDelta().uomCode()), entry.inventoryStatus(), entry.lotCode(),
                        entry.serialCode(), entry.expiryDate(), reversal.postedAt()))
                .toList();
        applyLedger(reversingLedger);
        repository.updateTransaction(new StockTransaction(original.id(), original.transactionNumber(),
                original.idempotencyKey(), original.movementType(), StockTransactionStatus.REVERSED,
                reversal.id(), original.lines(), original.postedAt(), original.businessDate(),
                original.actor()));
        repository.insertTransaction(reversal);
        repository.appendLedgerEntries(reversingLedger);
        audit.record(command.actor(), "INVENTORY_TRANSACTION_REVERSED", original.id());
        return reversal;
    }

    @Transactional(readOnly = true)
    public List<StockTransaction> transactions() {
        return repository.listTransactions();
    }

    @Transactional(readOnly = true)
    public List<StockBalance> balances(final UUID skuId) {
        return repository.listBalances(skuId);
    }

    private List<StockMovementLine> lines(final InventoryCommands.PostTransaction command) {
        return command.lines().stream().map(line -> {
            productCatalog.requireSku(line.item());
            if (line.fromLocation() != null) {
                warehouseReferences.requireLocation(line.fromLocation());
            }
            if (line.toLocation() != null) {
                warehouseReferences.requireLocation(line.toLocation());
            }
            return new StockMovementLine(UUID.randomUUID(), line.item(), line.fromLocation(), line.toLocation(),
                    line.quantity(), line.inventoryStatus(), line.lotCode(), line.serialCode(), line.expiryDate());
        }).toList();
    }

    private List<StockLedgerEntry> ledgerFor(final StockTransaction transaction) {
        final List<StockLedgerEntry> entries = new ArrayList<>();
        for (final StockMovementLine line : transaction.lines()) {
            if (requiresFrom(transaction.movementType())) {
                entries.add(entry(transaction, line, line.fromLocation(), negative(line.quantity())));
            }
            if (requiresTo(transaction.movementType())) {
                entries.add(entry(transaction, line, line.toLocation(), line.quantity()));
            }
        }
        return entries;
    }

    private StockLedgerEntry entry(final StockTransaction transaction, final StockMovementLine line,
                                   final InventoryLocation location, final InventoryQuantity delta) {
        if (location == null) {
            throw new InventoryConflictException("Stock movement location is required.");
        }
        if (delta.isNegative() && line.expiryDate() != null
                && line.expiryDate().isBefore(transaction.businessDate())) {
            throw new InventoryConflictException("Expired stock cannot be issued.");
        }
        return new StockLedgerEntry(UUID.randomUUID(), transaction.id(), line.id(), line.item().skuId(), location,
                delta, line.inventoryStatus(), line.lotCode(), line.serialCode(), line.expiryDate(),
                transaction.postedAt());
    }

    private void applyLedger(final List<StockLedgerEntry> entries) {
        for (final StockLedgerEntry entry : entries) {
            if (entry.inventoryStatus() != InventoryStatus.AVAILABLE && entry.quantityDelta().isNegative()) {
                throw new InventoryConflictException("Restricted stock cannot be issued normally.");
            }
            final StockBalance balance = balance(entry.skuId(), entry.location(), entry.inventoryStatus(),
                    entry.quantityDelta().uomCode());
            final InventoryQuantity nextOnHand = balance.onHandQuantity().add(entry.quantityDelta());
            if (nextOnHand.isNegative() && !configuration.negativeStockAllowed()) {
                throw new InventoryConflictException("Negative stock is not allowed.");
            }
            repository.upsertBalance(new StockBalance(balance.id(), balance.skuId(), balance.location(),
                    balance.inventoryStatus(), nextOnHand, balance.reservedQuantity(), balance.inTransitQuantity(),
                    entry.inventoryStatus() == InventoryStatus.DAMAGED ? nextOnHand : balance.damagedQuantity(),
                    entry.inventoryStatus() == InventoryStatus.QUARANTINE ? nextOnHand : balance.quarantineQuantity(),
                    balance.version() + 1));
        }
    }

    private StockBalance balance(final UUID skuId, final InventoryLocation location, final InventoryStatus status,
                                 final String uomCode) {
        return repository.findBalanceForUpdate(skuId, location, status).orElseGet(() ->
                new StockBalance(UUID.randomUUID(), skuId, location, status, InventoryQuantity.zero(uomCode),
                        InventoryQuantity.zero(uomCode), InventoryQuantity.zero(uomCode),
                        InventoryQuantity.zero(uomCode), InventoryQuantity.zero(uomCode), 0));
    }

    private void assertIdempotent(final String idempotencyKey) {
        if (repository.idempotencyKeyExists(idempotencyKey)) {
            throw new InventoryConflictException("Duplicate idempotency key: " + idempotencyKey);
        }
    }

    private static boolean requiresFrom(final MovementType type) {
        return type == MovementType.GOODS_ISSUE || type == MovementType.WAREHOUSE_TRANSFER
                || type == MovementType.BIN_TRANSFER;
    }

    private static boolean requiresTo(final MovementType type) {
        return type == MovementType.OPENING_BALANCE || type == MovementType.GOODS_RECEIPT
                || type == MovementType.WAREHOUSE_TRANSFER || type == MovementType.BIN_TRANSFER
                || type == MovementType.STOCK_ADJUSTMENT;
    }

    private static InventoryQuantity negative(final InventoryQuantity quantity) {
        return new InventoryQuantity(quantity.value().negate(), quantity.uomCode());
    }
}
