package com.newland.erp.identity.domain;

import java.time.Instant;
import java.util.UUID;

public record User(
        UUID id,
        Username username,
        EmailAddress email,
        String displayName,
        UserStatus status,
        int failedLoginAttempts,
        Instant lockedUntil,
        Instant passwordExpiresAt,
        Instant createdAt,
        Instant updatedAt
) {
    public User {
        Permission.require(id, "user id");
        Permission.require(username, "username");
        Permission.require(email, "email");
        if (displayName == null || displayName.isBlank() || displayName.length() > 160) {
            throw new IllegalArgumentException("Display name is required and must be at most 160 characters.");
        }
        Permission.require(status, "user status");
        Permission.require(createdAt, "created at");
        Permission.require(updatedAt, "updated at");
        displayName = displayName.trim();
    }

    public boolean canAuthenticate(final Instant now) {
        return status == UserStatus.ACTIVE && (lockedUntil == null || lockedUntil.isBefore(now));
    }

    public User recordFailedLogin(final int maxAttempts, final java.time.Duration lockDuration, final Instant now) {
        final int attempts = failedLoginAttempts + 1;
        if (attempts >= maxAttempts) {
            return new User(id, username, email, displayName, UserStatus.LOCKED, attempts, now.plus(lockDuration),
                    passwordExpiresAt, createdAt, now);
        }
        return new User(id, username, email, displayName, status, attempts, lockedUntil, passwordExpiresAt, createdAt,
                now);
    }

    public User recordSuccessfulLogin(final Instant now) {
        return new User(id, username, email, displayName, UserStatus.ACTIVE, 0, null, passwordExpiresAt, createdAt,
                now);
    }
}
