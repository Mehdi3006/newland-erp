package com.newland.erp.sales.domain;

import java.time.Instant;
import java.util.UUID;

public record SalesOrderRevision(UUID id, UUID salesOrderId, int revision, String reason,
                                 Instant createdAt, String actor) {
    public SalesOrderRevision {
        if (id == null || salesOrderId == null || revision < 1 || createdAt == null) {
            throw new IllegalArgumentException("Sales order revision identifiers are required.");
        }
        reason = SalesLine.required("revision reason", reason);
        actor = SalesLine.required("actor", actor);
    }
}
