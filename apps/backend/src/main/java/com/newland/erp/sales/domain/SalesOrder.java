package com.newland.erp.sales.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record SalesOrder(UUID id, String orderNumber, String idempotencyKey, UUID quotationId,
                         UUID customerId, UUID companyId, UUID branchId, UUID warehouseId, UUID salesChannelId,
                         UUID currencyId, SalesOrderStatus status, int revision, List<SalesOrderLine> lines,
                         int lockVersion, LocalDate requestedDeliveryDate, Instant createdAt, String actor) {
    public SalesOrder {
        if (id == null || customerId == null || companyId == null || branchId == null || warehouseId == null
                || salesChannelId == null || currencyId == null || status == null || createdAt == null) {
            throw new IllegalArgumentException("Sales order identifiers and status are required.");
        }
        orderNumber = SalesLine.required("orderNumber", orderNumber).toUpperCase();
        idempotencyKey = SalesLine.required("idempotencyKey", idempotencyKey);
        actor = SalesLine.required("actor", actor);
        lines = lines == null ? List.of() : List.copyOf(lines);
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Sales order requires at least one line.");
        }
        if (revision < 0) {
            throw new IllegalArgumentException("Sales order revision cannot be negative.");
        }
        if (lockVersion < 0) {
            throw new IllegalArgumentException("Sales order lock version cannot be negative.");
        }
    }

    public SalesOrder approve() {
        if (status != SalesOrderStatus.DRAFT) {
            throw new SalesConflictException("Only draft sales orders can be approved.");
        }
        return withStatus(SalesOrderStatus.APPROVED, lines);
    }

    public SalesOrder reserve(final UUID lineId, final SalesQuantity quantity) {
        requireFulfillable();
        final List<SalesOrderLine> nextLines = lines.stream()
                .map(line -> line.id().equals(lineId) ? line.reserve(quantity) : line).toList();
        return withStatus(SalesOrderStatus.PARTIALLY_RESERVED, nextLines);
    }

    public SalesOrder deliver(final UUID lineId, final SalesQuantity quantity) {
        requireFulfillable();
        return withProgress(lines.stream().map(line -> line.id().equals(lineId) ? line.deliver(quantity) : line)
                .toList(), SalesOrderStatus.PARTIALLY_DELIVERED);
    }

    public SalesOrder amend(final List<SalesOrderLine> revisedLines) {
        if (status != SalesOrderStatus.APPROVED) {
            throw new SalesConflictException("Only approved sales orders require controlled amendment.");
        }
        return new SalesOrder(id, orderNumber, idempotencyKey, quotationId, customerId, companyId, branchId,
                warehouseId, salesChannelId, currencyId, SalesOrderStatus.DRAFT, revision + 1, revisedLines,
                lockVersion, requestedDeliveryDate, createdAt, actor);
    }

    public SalesOrder cancel() {
        if (status == SalesOrderStatus.CANCELLED) {
            throw new SalesConflictException("Sales order is already cancelled.");
        }
        return withStatus(SalesOrderStatus.CANCELLED, lines.stream().map(SalesOrderLine::cancelRemaining).toList());
    }

    private void requireFulfillable() {
        if (status != SalesOrderStatus.APPROVED && status != SalesOrderStatus.PARTIALLY_RESERVED
                && status != SalesOrderStatus.PARTIALLY_DELIVERED) {
            throw new SalesConflictException("Only approved sales orders can be fulfilled.");
        }
    }

    private SalesOrder withProgress(final List<SalesOrderLine> nextLines, final SalesOrderStatus partialStatus) {
        final boolean complete = nextLines.stream().noneMatch(line -> line.remainingQuantity().isPositive());
        return withStatus(complete ? SalesOrderStatus.DELIVERED : partialStatus, nextLines);
    }

    private SalesOrder withStatus(final SalesOrderStatus nextStatus, final List<SalesOrderLine> nextLines) {
        return new SalesOrder(id, orderNumber, idempotencyKey, quotationId, customerId, companyId, branchId,
                warehouseId, salesChannelId, currencyId, nextStatus, revision, nextLines, lockVersion,
                requestedDeliveryDate, createdAt, actor);
    }

    public record SalesOrderLine(UUID id, UUID productId, UUID skuId, String skuCode, SalesQuantity orderedQuantity,
                                 SalesQuantity reservedQuantity, SalesQuantity deliveredQuantity,
                                 SalesQuantity cancelledQuantity, UUID taxCategoryId) {
        public SalesOrderLine {
            if (id == null || productId == null || skuId == null || orderedQuantity == null
                    || reservedQuantity == null || deliveredQuantity == null || cancelledQuantity == null
                    || !orderedQuantity.isPositive()) {
                throw new IllegalArgumentException("Sales order line identifiers and quantities are required.");
            }
            skuCode = SalesLine.required("skuCode", skuCode).toUpperCase();
            if (reservedQuantity.add(deliveredQuantity).add(cancelledQuantity).isGreaterThan(orderedQuantity)) {
                throw new SalesConflictException("Reserved plus delivered plus cancelled cannot exceed ordered.");
            }
        }

        public SalesQuantity remainingQuantity() {
            return orderedQuantity.subtract(reservedQuantity).subtract(deliveredQuantity).subtract(cancelledQuantity);
        }

        public SalesOrderLine reserve(final SalesQuantity quantity) {
            return new SalesOrderLine(id, productId, skuId, skuCode, orderedQuantity, reservedQuantity.add(quantity),
                    deliveredQuantity, cancelledQuantity, taxCategoryId);
        }

        public SalesOrderLine deliver(final SalesQuantity quantity) {
            return new SalesOrderLine(id, productId, skuId, skuCode, orderedQuantity, reservedQuantity,
                    deliveredQuantity.add(quantity), cancelledQuantity, taxCategoryId);
        }

        public SalesOrderLine cancelRemaining() {
            return new SalesOrderLine(id, productId, skuId, skuCode, orderedQuantity, reservedQuantity,
                    deliveredQuantity, remainingQuantity(), taxCategoryId);
        }
    }
}
