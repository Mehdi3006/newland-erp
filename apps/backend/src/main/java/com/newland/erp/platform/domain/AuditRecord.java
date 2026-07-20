package com.newland.erp.platform.domain;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuditRecord(UUID id, String actor, String action, String targetType, UUID targetId, Instant occurredAt,
                          Map<String, String> attributes) {
    public AuditRecord {
        PlatformDomainEvent.require(id, "audit id");
        PlatformDomainEvent.requireText(actor, "actor");
        PlatformDomainEvent.requireText(action, "action");
        PlatformDomainEvent.requireText(targetType, "target type");
        PlatformDomainEvent.require(targetId, "target id");
        PlatformDomainEvent.require(occurredAt, "occurred at");
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}
