package com.newland.erp.identity.domain;

import java.time.Instant;
import java.util.UUID;

public record RefreshToken(UUID id, UUID sessionId, UUID userId, String tokenHash, Instant issuedAt, Instant expiresAt,
                           Instant rotatedAt, Instant revokedAt) {
    public RefreshToken {
        Permission.require(id, "refresh token id");
        Permission.require(sessionId, "session id");
        Permission.require(userId, "user id");
        if (tokenHash == null || tokenHash.isBlank()) {
            throw new IllegalArgumentException("Refresh token hash is required.");
        }
        Permission.require(issuedAt, "issued at");
        Permission.require(expiresAt, "expires at");
    }

    public boolean usable(final Instant now) {
        return revokedAt == null && rotatedAt == null && expiresAt.isAfter(now);
    }
}
