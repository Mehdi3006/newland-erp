package com.newland.erp.platform.domain;

public record ErrorCatalogEntry(String code, String httpStatus, String title, String ownerContext) {
    public ErrorCatalogEntry {
        PlatformDomainEvent.requireText(code, "error code");
        PlatformDomainEvent.requireText(httpStatus, "HTTP status");
        PlatformDomainEvent.requireText(title, "title");
        PlatformDomainEvent.requireText(ownerContext, "owner context");
    }
}
