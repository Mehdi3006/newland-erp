package com.newland.erp.inventory.application;

import com.newland.erp.inventory.domain.InventoryItemReference;
import com.newland.erp.inventory.domain.InventoryLocation;
import com.newland.erp.inventory.domain.InventoryStatus;
import com.newland.erp.inventory.domain.InventoryQuantity;
import com.newland.erp.inventory.domain.MovementType;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class InventoryCommands {
    public record PostLine(InventoryItemReference item, InventoryLocation fromLocation, InventoryLocation toLocation,
                           InventoryQuantity quantity, InventoryStatus inventoryStatus, String lotCode,
                           String serialCode, LocalDate expiryDate) {
    }

    public record PostTransaction(MovementType movementType, String idempotencyKey, LocalDate businessDate,
                                  List<PostLine> lines, List<UUID> attachmentIds, String actor) {
        public PostTransaction {
            lines = lines == null ? List.of() : List.copyOf(lines);
            attachmentIds = attachmentIds == null ? List.of() : List.copyOf(attachmentIds);
        }
    }

    public record Reserve(InventoryItemReference item, InventoryLocation location, InventoryQuantity quantity,
                          String idempotencyKey, String actor) {
    }

    public record Release(UUID reservationId, String idempotencyKey, String actor) {
    }

    public record Reverse(UUID transactionId, String idempotencyKey, String actor) {
    }

    private InventoryCommands() {
    }
}
