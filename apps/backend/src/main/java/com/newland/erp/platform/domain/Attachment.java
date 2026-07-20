package com.newland.erp.platform.domain;

import java.time.Instant;
import java.util.UUID;

public record Attachment(UUID id, String ownerContext, String ownerType, UUID ownerId, UUID fileId,
                         Instant attachedAt) {
    public Attachment {
        PlatformDomainEvent.require(id, "attachment id");
        PlatformDomainEvent.requireText(ownerContext, "owner context");
        PlatformDomainEvent.requireText(ownerType, "owner type");
        PlatformDomainEvent.require(ownerId, "owner id");
        PlatformDomainEvent.require(fileId, "file id");
        PlatformDomainEvent.require(attachedAt, "attached at");
    }
}
