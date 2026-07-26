package com.newland.erp.platform.domain;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record OutboxMessage(UUID id, PlatformDomainEvent event, OutboxStatus status, int attempts,
                            Instant nextAttemptAt, Instant createdAt, Instant publishedAt, String lastError) {
    public OutboxMessage {
        PlatformDomainEvent.require(id, "outbox id");
        PlatformDomainEvent.require(event, "event");
        PlatformDomainEvent.require(status, "outbox status");
        PlatformDomainEvent.require(createdAt, "created at");
        if (attempts < 0) {
            throw new IllegalArgumentException("attempts cannot be negative.");
        }
    }

    public static OutboxMessage pending(final PlatformDomainEvent event, final Instant now) {
        return new OutboxMessage(UUID.randomUUID(), event, OutboxStatus.PENDING, 0, now, now, null, null);
    }

    public OutboxMessage published(final Instant now) {
        return new OutboxMessage(id, event, OutboxStatus.PUBLISHED, attempts, nextAttemptAt, createdAt, now, null);
    }

    public OutboxMessage failed(final Instant nextAttempt, final String error) {
        return new OutboxMessage(id, event, OutboxStatus.FAILED, attempts, nextAttempt, createdAt, null, error);
    }

    public Map<String, String> payload() {
        return event.payload();
    }
}
