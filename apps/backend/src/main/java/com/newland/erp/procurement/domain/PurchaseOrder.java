package com.newland.erp.procurement.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PurchaseOrder(UUID id, String orderNumber, String idempotencyKey, UUID requisitionId,
                            UUID supplierId, UUID companyId, UUID branchId, UUID warehouseId,
                            UUID currencyId, PurchaseOrderStatus status, int revision,
                            List<PurchaseOrderLine> lines, LocalDate expectedDeliveryDate, Instant createdAt,
                            String actor) {
    public PurchaseOrder {
        if (id == null || supplierId == null || companyId == null || branchId == null || warehouseId == null
                || currencyId == null || status == null || createdAt == null) {
            throw new IllegalArgumentException("Purchase order identifiers and status are required.");
        }
        orderNumber = PurchaseRequisition.required("orderNumber", orderNumber).toUpperCase();
        idempotencyKey = PurchaseRequisition.required("idempotencyKey", idempotencyKey);
        actor = PurchaseRequisition.required("actor", actor);
        lines = lines == null ? List.of() : List.copyOf(lines);
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Purchase order requires at least one line.");
        }
        if (revision < 0) {
            throw new IllegalArgumentException("Purchase order revision cannot be negative.");
        }
    }

    public PurchaseOrder approve() {
        if (status != PurchaseOrderStatus.DRAFT) {
            throw new ProcurementConflictException("Only draft purchase orders can be approved.");
        }
        return withStatus(PurchaseOrderStatus.APPROVED, lines);
    }

    public PurchaseOrder receive(final UUID lineId, final ProcurementQuantity quantity) {
        final List<PurchaseOrderLine> updated = lines.stream()
                .map(line -> line.id().equals(lineId) ? line.receive(quantity) : line)
                .toList();
        final boolean anyRemaining = updated.stream().anyMatch(line -> line.remainingQuantity().isPositive());
        return withStatus(anyRemaining ? PurchaseOrderStatus.PARTIALLY_RECEIVED : PurchaseOrderStatus.RECEIVED,
                updated);
    }

    public PurchaseOrder cancel() {
        final List<PurchaseOrderLine> updated = lines.stream().map(PurchaseOrderLine::cancelRemaining).toList();
        return withStatus(PurchaseOrderStatus.CANCELLED, updated);
    }

    public PurchaseOrder amend(final List<PurchaseOrderLine> revisedLines) {
        if (status != PurchaseOrderStatus.APPROVED) {
            throw new ProcurementConflictException("Only approved purchase orders require controlled amendment.");
        }
        return new PurchaseOrder(id, orderNumber, idempotencyKey, requisitionId, supplierId, companyId, branchId,
                warehouseId, currencyId, PurchaseOrderStatus.DRAFT, revision + 1, revisedLines,
                expectedDeliveryDate, createdAt, actor);
    }

    private PurchaseOrder withStatus(final PurchaseOrderStatus nextStatus, final List<PurchaseOrderLine> nextLines) {
        return new PurchaseOrder(id, orderNumber, idempotencyKey, requisitionId, supplierId, companyId, branchId,
                warehouseId, currencyId, nextStatus, revision, nextLines, expectedDeliveryDate, createdAt, actor);
    }

    public record PurchaseOrderLine(UUID id, UUID productId, UUID skuId, String skuCode,
                                    ProcurementQuantity orderedQuantity, ProcurementQuantity receivedQuantity,
                                    ProcurementQuantity cancelledQuantity, UUID taxCategoryId) {
        public PurchaseOrderLine {
            if (id == null || productId == null || skuId == null || orderedQuantity == null
                    || receivedQuantity == null || cancelledQuantity == null || !orderedQuantity.isPositive()) {
                throw new IllegalArgumentException("Purchase order line identifiers and quantities are required.");
            }
            skuCode = PurchaseRequisition.required("skuCode", skuCode).toUpperCase();
            if (receivedQuantity.add(cancelledQuantity).isGreaterThan(orderedQuantity)) {
                throw new ProcurementConflictException("Received plus cancelled quantity cannot exceed ordered.");
            }
        }

        public ProcurementQuantity remainingQuantity() {
            return orderedQuantity.subtract(receivedQuantity).subtract(cancelledQuantity);
        }

        public PurchaseOrderLine receive(final ProcurementQuantity quantity) {
            final ProcurementQuantity nextReceived = receivedQuantity.add(quantity);
            return new PurchaseOrderLine(id, productId, skuId, skuCode, orderedQuantity, nextReceived,
                    cancelledQuantity, taxCategoryId);
        }

        public PurchaseOrderLine cancelRemaining() {
            return new PurchaseOrderLine(id, productId, skuId, skuCode, orderedQuantity, receivedQuantity,
                    remainingQuantity(), taxCategoryId);
        }
    }
}
