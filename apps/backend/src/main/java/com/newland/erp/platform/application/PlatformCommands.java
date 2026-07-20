package com.newland.erp.platform.application;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public final class PlatformCommands {
    public record PublishEvent(String sourceContext, String eventType, UUID aggregateId,
                               Map<String, String> payload) {
    }

    public record RecordAudit(String actor, String action, String targetType, UUID targetId,
                              Map<String, String> attributes) {
    }

    public record ScheduleJob(String jobType, Instant scheduledAt, Map<String, String> parameters) {
    }

    public record RegisterStoredFile(String fileName, String contentType, long sizeBytes, String checksumSha256) {
    }

    public record StoreFile(String fileName, String contentType, byte[] content, String checksumSha256) {
    }

    public record AttachFile(String ownerContext, String ownerType, UUID ownerId, UUID fileId) {
    }

    public record SetConfiguration(String key, String value, boolean encrypted, String actor) {
    }

    public record SetFeatureFlag(String key, boolean enabled, String description, String actor) {
    }

    public record SetLocalization(String locale, String messageKey, String message) {
    }

    private PlatformCommands() {
    }
}
