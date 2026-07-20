package com.newland.erp.identity.application;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record IdentityEvent(UUID eventId, String eventType, UUID aggregateId, Instant occurredAt,
                            Map<String, String> payload) {
}
