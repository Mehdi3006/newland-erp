package com.newland.erp.enterprise.application;

import java.time.Instant;
import java.util.UUID;

public record EnterpriseStructureAuditEvent(
        UUID eventId,
        String eventType,
        UUID aggregateId,
        String actor,
        UUID correlationId,
        Instant occurredAt
) {
}
