package com.newland.erp.platform.domain;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record PlatformDomainEvent(UUID eventId, String sourceContext, String eventType, UUID aggregateId,
                                  Instant occurredAt, Map<String, String> payload) {
    public PlatformDomainEvent {
        require(eventId, "event id");
        requireText(sourceContext, "source context");
        requireText(eventType, "event type");
        require(aggregateId, "aggregate id");
        require(occurredAt, "occurred at");
        payload = payload == null ? Map.of() : Map.copyOf(payload);
    }

    static void require(final Object value, final String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " is required.");
        }
    }

    static void requireText(final String value, final String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required.");
        }
    }
}
