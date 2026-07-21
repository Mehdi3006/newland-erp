package com.newland.erp.procurement.domain;

import java.time.Instant;
import java.util.UUID;

public record PurchaseOrderRevision(UUID id, UUID purchaseOrderId, int revision, String reason,
                                    Instant createdAt, String actor) {
    public PurchaseOrderRevision {
        if (id == null || purchaseOrderId == null || createdAt == null || revision < 1) {
            throw new IllegalArgumentException("Purchase order revision identifiers are required.");
        }
        reason = PurchaseRequisition.required("reason", reason);
        actor = PurchaseRequisition.required("actor", actor);
    }
}
