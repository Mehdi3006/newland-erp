package com.newland.erp.platform.domain;

import java.time.Instant;

public record FeatureFlag(String key, boolean enabled, String description, Instant updatedAt, String updatedBy) {
    public FeatureFlag {
        PlatformDomainEvent.requireText(key, "feature flag key");
        PlatformDomainEvent.require(updatedAt, "updated at");
        PlatformDomainEvent.requireText(updatedBy, "updated by");
    }
}
