package com.newland.erp.identity.domain;

public record Capability(String value) {
    public Capability {
        if (value == null || !value.matches("[a-z][a-z0-9-]*(\\.[a-z][a-z0-9-]*)+")) {
            throw new IllegalArgumentException("Capability must use dotted lower-case form.");
        }
    }
}
