package com.newland.erp.inventory.application;

import com.newland.erp.inventory.domain.InventoryLocation;
import com.newland.erp.inventory.domain.InventoryStatus;
import com.newland.erp.inventory.domain.Reservation;
import com.newland.erp.inventory.domain.StockBalance;
import com.newland.erp.inventory.domain.StockLedgerEntry;
import com.newland.erp.inventory.domain.StockTransaction;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryRepository {
    boolean idempotencyKeyExists(String idempotencyKey);

    StockTransaction insertTransaction(StockTransaction transaction);

    StockTransaction updateTransaction(StockTransaction transaction);

    Optional<StockTransaction> findTransaction(UUID transactionId);

    List<StockTransaction> listTransactions();

    void appendLedgerEntries(List<StockLedgerEntry> entries);

    Optional<StockBalance> findBalanceForUpdate(UUID skuId, InventoryLocation location, InventoryStatus status);

    StockBalance upsertBalance(StockBalance balance);

    List<StockBalance> listBalances(UUID skuId);

    Reservation insertReservation(Reservation reservation);

    Reservation updateReservation(Reservation reservation);

    Optional<Reservation> findReservation(UUID reservationId);
}
