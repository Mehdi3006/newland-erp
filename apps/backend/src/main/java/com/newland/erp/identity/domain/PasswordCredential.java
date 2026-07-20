package com.newland.erp.identity.domain;

import java.time.Instant;
import java.util.UUID;

public record PasswordCredential(UUID id, UUID userId, String passwordHash, Instant changedAt, Instant expiresAt) {
    public PasswordCredential {
        Permission.require(id, "password credential id");
        Permission.require(userId, "user id");
        if (passwordHash == null || passwordHash.isBlank() || !passwordHash.startsWith("$argon2")) {
            throw new IllegalArgumentException("Password hash must be Argon2 encoded.");
        }
        Permission.require(changedAt, "changed at");
    }
}
