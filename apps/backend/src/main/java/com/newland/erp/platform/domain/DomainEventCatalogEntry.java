package com.newland.erp.platform.domain;

public record DomainEventCatalogEntry(String eventType, String ownerContext, String description) {
    public DomainEventCatalogEntry {
        PlatformDomainEvent.requireText(eventType, "event type");
        PlatformDomainEvent.requireText(ownerContext, "owner context");
        PlatformDomainEvent.requireText(description, "description");
    }
}
