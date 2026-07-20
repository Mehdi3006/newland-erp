package com.newland.erp.enterprise.domain;

import java.time.Instant;

public record AuditMetadata(
        Instant createdAt,
        String createdBy,
        Instant updatedAt,
        String updatedBy,
        long version
) {
    public AuditMetadata {
        if (createdAt == null || updatedAt == null) {
            throw new IllegalArgumentException("Audit timestamps are required.");
        }
        createdBy = TextValue.required(createdBy, "createdBy", 120);
        updatedBy = TextValue.required(updatedBy, "updatedBy", 120);
        if (version < 0) {
            throw new IllegalArgumentException("version cannot be negative.");
        }
    }

    public static AuditMetadata created(final Instant occurredAt, final String actor) {
        return new AuditMetadata(occurredAt, actor, occurredAt, actor, 0L);
    }

    public AuditMetadata touched(final Instant occurredAt, final String actor) {
        return new AuditMetadata(createdAt, createdBy, occurredAt, actor, version + 1L);
    }
}
