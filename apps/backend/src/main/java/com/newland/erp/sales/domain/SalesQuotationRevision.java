package com.newland.erp.sales.domain;

import java.time.Instant;
import java.util.UUID;

public record SalesQuotationRevision(UUID id, UUID quotationId, int revision, String reason,
                                     Instant createdAt, String actor) {
    public SalesQuotationRevision {
        if (id == null || quotationId == null || revision < 1 || createdAt == null) {
            throw new IllegalArgumentException("Sales quotation revision identifiers are required.");
        }
        reason = SalesLine.required("revision reason", reason);
        actor = SalesLine.required("actor", actor);
    }
}
