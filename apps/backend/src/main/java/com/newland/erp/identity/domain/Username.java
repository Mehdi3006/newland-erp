package com.newland.erp.identity.domain;

import java.util.Locale;

public record Username(String value) {
    public Username {
        if (value == null || value.isBlank() || value.length() > 120) {
            throw new IllegalArgumentException("Username is required and must be at most 120 characters.");
        }
        value = value.trim().toLowerCase(Locale.ROOT);
    }
}
