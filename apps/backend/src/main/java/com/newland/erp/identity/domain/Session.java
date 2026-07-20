package com.newland.erp.identity.domain;

import java.time.Instant;
import java.util.UUID;

public record Session(UUID id, UUID userId, String deviceLabel, Instant createdAt, Instant expiresAt,
                      Instant revokedAt) {
    public Session {
        Permission.require(id, "session id");
        Permission.require(userId, "user id");
        Permission.require(createdAt, "created at");
        Permission.require(expiresAt, "expires at");
    }

    public boolean active(final Instant now) {
        return revokedAt == null && expiresAt.isAfter(now);
    }
}
