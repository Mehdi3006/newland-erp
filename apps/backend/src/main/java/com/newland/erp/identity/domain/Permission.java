package com.newland.erp.identity.domain;

import java.util.UUID;

public record Permission(UUID id, Capability capability, String description) {
    public Permission {
        require(id, "permission id");
        require(capability, "permission capability");
    }

    static void require(final Object value, final String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " is required.");
        }
    }
}
