package com.newland.erp.platform.domain;

import java.time.Instant;
import java.util.UUID;

public record StoredFile(UUID id, String storageKey, String fileName, String contentType, long sizeBytes,
                         String checksumSha256, Instant createdAt) {
    public StoredFile {
        PlatformDomainEvent.require(id, "file id");
        PlatformDomainEvent.requireText(storageKey, "storage key");
        PlatformDomainEvent.requireText(fileName, "file name");
        PlatformDomainEvent.requireText(contentType, "content type");
        if (sizeBytes < 0) {
            throw new IllegalArgumentException("file size cannot be negative.");
        }
        PlatformDomainEvent.requireText(checksumSha256, "checksum");
        PlatformDomainEvent.require(createdAt, "created at");
    }
}
