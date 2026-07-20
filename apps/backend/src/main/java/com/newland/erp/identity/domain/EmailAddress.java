package com.newland.erp.identity.domain;

import java.util.Locale;

public record EmailAddress(String value) {
    public EmailAddress {
        if (value == null || !value.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$") || value.length() > 160) {
            throw new IllegalArgumentException("Valid email address is required.");
        }
        value = value.trim().toLowerCase(Locale.ROOT);
    }
}
