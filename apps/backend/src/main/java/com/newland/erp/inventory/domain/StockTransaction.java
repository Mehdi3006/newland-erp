package com.newland.erp.inventory.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record StockTransaction(UUID id, String transactionNumber, String idempotencyKey,
                               MovementType movementType, StockTransactionStatus status,
                               UUID reversedTransactionId, List<StockMovementLine> lines,
                               Instant postedAt, LocalDate businessDate, String actor) {
    public StockTransaction {
        if (id == null) {
            throw new IllegalArgumentException("Stock transaction id is required.");
        }
        transactionNumber = required("transactionNumber", transactionNumber).toUpperCase();
        idempotencyKey = required("idempotencyKey", idempotencyKey);
        if (movementType == null || status == null) {
            throw new IllegalArgumentException("Stock transaction movement type and status are required.");
        }
        lines = lines == null ? List.of() : List.copyOf(lines);
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Stock transaction requires at least one line.");
        }
        if (postedAt == null || businessDate == null) {
            throw new IllegalArgumentException("Stock transaction posting timestamps are required.");
        }
        actor = required("actor", actor);
    }

    public StockTransaction reversed(final String reversalNumber, final String reversalIdempotencyKey,
                                     final Instant reversalPostedAt, final String reversalActor) {
        final List<StockMovementLine> reversalLines = lines.stream()
                .map(line -> new StockMovementLine(UUID.randomUUID(), line.item(), line.fromLocation(),
                        line.toLocation(), line.quantity(), line.inventoryStatus(), line.lotCode(),
                        line.serialCode(), line.expiryDate()))
                .toList();
        return new StockTransaction(UUID.randomUUID(), reversalNumber, reversalIdempotencyKey, MovementType.REVERSAL,
                StockTransactionStatus.POSTED, id, reversalLines, reversalPostedAt, businessDate, reversalActor);
    }

    private static String required(final String name, final String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Stock transaction " + name + " is required.");
        }
        return value.trim();
    }
}
