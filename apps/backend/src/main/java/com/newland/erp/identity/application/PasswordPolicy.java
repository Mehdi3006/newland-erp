package com.newland.erp.identity.application;

import com.newland.erp.identity.domain.AuthenticationFailedException;

import java.time.Duration;

public record PasswordPolicy(int minLength, int maxFailedAttempts, Duration lockDuration, Duration passwordTtl) {
    public PasswordPolicy {
        if (minLength < 12) {
            throw new IllegalArgumentException("Minimum password length must be at least 12.");
        }
        if (maxFailedAttempts < 3) {
            throw new IllegalArgumentException("Maximum failed attempts must be at least 3.");
        }
    }

    public void validate(final String password) {
        if (password == null || password.length() < minLength
                || !password.matches(".*[A-Z].*")
                || !password.matches(".*[a-z].*")
                || !password.matches(".*[0-9].*")) {
            throw new AuthenticationFailedException("Password does not satisfy the active password policy.");
        }
    }

    public static PasswordPolicy enterpriseDefault() {
        return new PasswordPolicy(12, 5, Duration.ofMinutes(15), Duration.ofDays(90));
    }
}
