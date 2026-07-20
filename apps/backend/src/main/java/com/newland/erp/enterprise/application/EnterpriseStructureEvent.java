package com.newland.erp.enterprise.application;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record EnterpriseStructureEvent(
        UUID eventId,
        String eventType,
        UUID aggregateId,
        Instant occurredAt,
        String actor,
        UUID correlationId,
        Map<String, String> payload
) {
}
