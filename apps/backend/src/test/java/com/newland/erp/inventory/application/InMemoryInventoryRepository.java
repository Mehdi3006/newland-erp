package com.newland.erp.inventory.application;

import com.newland.erp.inventory.domain.InventoryLocation;
import com.newland.erp.inventory.domain.InventoryStatus;
import com.newland.erp.inventory.domain.Reservation;
import com.newland.erp.inventory.domain.StockBalance;
import com.newland.erp.inventory.domain.StockLedgerEntry;
import com.newland.erp.inventory.domain.StockTransaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

final class InMemoryInventoryRepository implements InventoryRepository {
    final List<StockTransaction> transactions = new ArrayList<>();
    final List<StockLedgerEntry> ledger = new ArrayList<>();
    final List<StockBalance> balances = new ArrayList<>();
    final List<Reservation> reservations = new ArrayList<>();

    @Override
    public boolean idempotencyKeyExists(final String idempotencyKey) {
        return transactions.stream().anyMatch(tx -> tx.idempotencyKey().equals(idempotencyKey))
                || reservations.stream().anyMatch(reservation -> reservation.idempotencyKey().equals(idempotencyKey));
    }

    @Override
    public StockTransaction insertTransaction(final StockTransaction transaction) {
        transactions.add(transaction);
        return transaction;
    }

    @Override
    public StockTransaction updateTransaction(final StockTransaction transaction) {
        transactions.removeIf(tx -> tx.id().equals(transaction.id()));
        transactions.add(transaction);
        return transaction;
    }

    @Override
    public Optional<StockTransaction> findTransaction(final UUID transactionId) {
        return transactions.stream().filter(tx -> tx.id().equals(transactionId)).findFirst();
    }

    @Override
    public List<StockTransaction> listTransactions() {
        return List.copyOf(transactions);
    }

    @Override
    public void appendLedgerEntries(final List<StockLedgerEntry> entries) {
        ledger.addAll(entries);
    }

    @Override
    public Optional<StockBalance> findBalanceForUpdate(final UUID skuId, final InventoryLocation location,
                                                       final InventoryStatus status) {
        return balances.stream().filter(balance -> balance.skuId().equals(skuId)
                && balance.location().equals(location) && balance.inventoryStatus() == status).findFirst();
    }

    @Override
    public StockBalance upsertBalance(final StockBalance balance) {
        balances.removeIf(existing -> existing.skuId().equals(balance.skuId())
                && existing.location().equals(balance.location())
                && existing.inventoryStatus() == balance.inventoryStatus());
        balances.add(balance);
        return balance;
    }

    @Override
    public List<StockBalance> listBalances(final UUID skuId) {
        return balances.stream().filter(balance -> balance.skuId().equals(skuId)).toList();
    }

    @Override
    public Reservation insertReservation(final Reservation reservation) {
        reservations.add(reservation);
        return reservation;
    }

    @Override
    public Reservation updateReservation(final Reservation reservation) {
        reservations.removeIf(existing -> existing.id().equals(reservation.id()));
        reservations.add(reservation);
        return reservation;
    }

    @Override
    public Optional<Reservation> findReservation(final UUID reservationId) {
        return reservations.stream().filter(reservation -> reservation.id().equals(reservationId)).findFirst();
    }
}
