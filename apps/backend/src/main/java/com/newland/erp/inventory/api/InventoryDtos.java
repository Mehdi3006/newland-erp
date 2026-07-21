package com.newland.erp.inventory.api;

import com.newland.erp.inventory.domain.InventoryItemReference;
import com.newland.erp.inventory.domain.InventoryLocation;
import com.newland.erp.inventory.domain.InventoryQuantity;
import com.newland.erp.inventory.domain.InventoryStatus;
import com.newland.erp.inventory.domain.MovementType;
import com.newland.erp.inventory.domain.StockBalance;
import com.newland.erp.inventory.domain.StockTransaction;
import com.newland.erp.inventory.domain.TrackingPolicy;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class InventoryDtos {
    public record ItemRequest(@NotNull UUID productId, @NotNull UUID skuId, @NotBlank String skuCode,
                              @NotBlank String uomCode, TrackingPolicy trackingPolicy) {
        InventoryItemReference toDomain() {
            return new InventoryItemReference(productId, skuId, skuCode, uomCode, trackingPolicy);
        }
    }

    public record LocationRequest(@NotNull UUID warehouseId, UUID zoneId, UUID binId) {
        InventoryLocation toDomain() {
            return new InventoryLocation(warehouseId, zoneId, binId);
        }
    }

    public record QuantityRequest(@NotNull @Positive BigDecimal value, @NotBlank String uomCode) {
        InventoryQuantity toDomain() {
            return new InventoryQuantity(value, uomCode);
        }
    }

    public record PostLineRequest(@Valid @NotNull ItemRequest item, @Valid LocationRequest fromLocation,
                                  @Valid LocationRequest toLocation, @Valid @NotNull QuantityRequest quantity,
                                  InventoryStatus inventoryStatus, String lotCode, String serialCode,
                                  LocalDate expiryDate) {
    }

    public record PostTransactionRequest(@NotNull MovementType movementType, @NotBlank String idempotencyKey,
                                         @NotNull LocalDate businessDate,
                                         @NotEmpty List<@Valid PostLineRequest> lines,
                                         List<UUID> attachmentIds) {
    }

    public record ReserveRequest(@Valid @NotNull ItemRequest item, @Valid @NotNull LocationRequest location,
                                 @Valid @NotNull QuantityRequest quantity, @NotBlank String idempotencyKey) {
    }

    public record ReleaseRequest(@NotBlank String idempotencyKey) {
    }

    public record ReverseRequest(@NotBlank String idempotencyKey) {
    }

    public record TransactionResponse(UUID id, String transactionNumber, String movementType, String status,
                                      LocalDate businessDate) {
        static TransactionResponse from(final StockTransaction transaction) {
            return new TransactionResponse(transaction.id(), transaction.transactionNumber(),
                    transaction.movementType().name(), transaction.status().name(), transaction.businessDate());
        }
    }

    public record BalanceResponse(UUID skuId, UUID warehouseId, UUID zoneId, UUID binId,
                                  String inventoryStatus, BigDecimal onHandQuantity, BigDecimal reservedQuantity,
                                  BigDecimal availableQuantity, String uomCode, long version) {
        static BalanceResponse from(final StockBalance balance) {
            return new BalanceResponse(balance.skuId(), balance.location().warehouseId(), balance.location().zoneId(),
                    balance.location().binId(), balance.inventoryStatus().name(), balance.onHandQuantity().value(),
                    balance.reservedQuantity().value(), balance.availableQuantity().value(),
                    balance.onHandQuantity().uomCode(), balance.version());
        }
    }

    private InventoryDtos() {
    }
}
