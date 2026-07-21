package com.newland.erp.procurement.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PurchaseRequisition(UUID id, String requisitionNumber, String idempotencyKey,
                                  UUID companyId, UUID branchId, UUID warehouseId,
                                  RequisitionStatus status, int revision, List<ProcurementLine> lines,
                                  Instant createdAt, String actor) {
    public PurchaseRequisition {
        if (id == null || companyId == null || branchId == null || warehouseId == null || status == null
                || createdAt == null) {
            throw new IllegalArgumentException("Purchase requisition identifiers and status are required.");
        }
        requisitionNumber = required("requisitionNumber", requisitionNumber).toUpperCase();
        idempotencyKey = required("idempotencyKey", idempotencyKey);
        actor = required("actor", actor);
        lines = lines == null ? List.of() : List.copyOf(lines);
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Purchase requisition requires at least one line.");
        }
        if (revision < 0) {
            throw new IllegalArgumentException("Purchase requisition revision cannot be negative.");
        }
    }

    public PurchaseRequisition approve() {
        if (status != RequisitionStatus.SUBMITTED) {
            throw new ProcurementConflictException("Only submitted requisitions can be approved.");
        }
        return withStatus(RequisitionStatus.APPROVED);
    }

    public PurchaseRequisition reject() {
        if (status != RequisitionStatus.SUBMITTED) {
            throw new ProcurementConflictException("Only submitted requisitions can be rejected.");
        }
        return withStatus(RequisitionStatus.REJECTED);
    }

    public PurchaseRequisition resubmit(final String key) {
        if (status != RequisitionStatus.REJECTED) {
            throw new ProcurementConflictException("Only rejected requisitions can be resubmitted.");
        }
        return new PurchaseRequisition(id, requisitionNumber, key, companyId, branchId, warehouseId,
                RequisitionStatus.SUBMITTED, revision + 1, lines, createdAt, actor);
    }

    private PurchaseRequisition withStatus(final RequisitionStatus nextStatus) {
        return new PurchaseRequisition(id, requisitionNumber, idempotencyKey, companyId, branchId, warehouseId,
                nextStatus, revision, lines, createdAt, actor);
    }

    static String required(final String name, final String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Purchase requisition " + name + " is required.");
        }
        return value.trim();
    }
}
