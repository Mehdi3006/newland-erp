package com.newland.erp.identity.domain;

import java.util.UUID;

public record Role(UUID id, String code, String name, String description, boolean systemRole) {
    public Role {
        Permission.require(id, "role id");
        code = normalizeCode(code);
        if (name == null || name.isBlank() || name.length() > 160) {
            throw new IllegalArgumentException("Role name is required and must be at most 160 characters.");
        }
        name = name.trim();
    }

    private static String normalizeCode(final String value) {
        if (value == null || !value.trim().matches("[A-Za-z][A-Za-z0-9_-]{1,63}")) {
            throw new IllegalArgumentException("Role code must be 2-64 alphanumeric characters.");
        }
        return value.trim().toUpperCase(java.util.Locale.ROOT);
    }
}
