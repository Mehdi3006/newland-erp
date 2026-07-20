package com.newland.erp.enterprise.application;

import java.util.UUID;

public record RequestMetadata(String actor, UUID correlationId) {
    public RequestMetadata {
        if (actor == null || actor.isBlank()) {
            throw new IllegalArgumentException("actor is required.");
        }
        if (correlationId == null) {
            throw new IllegalArgumentException("correlationId is required.");
        }
    }
}
