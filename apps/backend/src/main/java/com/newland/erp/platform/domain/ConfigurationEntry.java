package com.newland.erp.platform.domain;

import java.time.Instant;

public record ConfigurationEntry(String key, String value, boolean encrypted, Instant updatedAt, String updatedBy) {
    public ConfigurationEntry {
        PlatformDomainEvent.requireText(key, "configuration key");
        PlatformDomainEvent.require(value, "configuration value");
        PlatformDomainEvent.require(updatedAt, "updated at");
        PlatformDomainEvent.requireText(updatedBy, "updated by");
    }
}
