package com.newland.erp.procurement.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RequestForQuotation(UUID id, String rfqNumber, String idempotencyKey, UUID requisitionId,
                                  RfqStatus status, List<UUID> invitedSupplierIds, Instant createdAt) {
    public RequestForQuotation {
        if (id == null || requisitionId == null || status == null || createdAt == null) {
            throw new IllegalArgumentException("RFQ identifiers and status are required.");
        }
        rfqNumber = PurchaseRequisition.required("rfqNumber", rfqNumber).toUpperCase();
        idempotencyKey = PurchaseRequisition.required("idempotencyKey", idempotencyKey);
        invitedSupplierIds = invitedSupplierIds == null ? List.of() : List.copyOf(invitedSupplierIds);
        if (invitedSupplierIds.isEmpty()) {
            throw new ProcurementConflictException("RFQ must invite at least one supplier.");
        }
    }
}
