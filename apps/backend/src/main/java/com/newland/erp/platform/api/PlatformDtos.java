package com.newland.erp.platform.api;

import com.newland.erp.platform.domain.BackgroundJob;
import com.newland.erp.platform.domain.DomainEventCatalogEntry;
import com.newland.erp.platform.domain.ErrorCatalogEntry;
import com.newland.erp.platform.domain.OutboxMessage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public final class PlatformDtos {
    public record PublishEventRequest(@NotBlank String sourceContext, @NotBlank String eventType,
                                      @NotNull UUID aggregateId, Map<String, String> payload) {
    }

    public record RecordAuditRequest(@NotBlank String actor, @NotBlank String action, @NotBlank String targetType,
                                     @NotNull UUID targetId, Map<String, String> attributes) {
    }

    public record ScheduleJobRequest(@NotBlank String jobType, @NotNull Instant scheduledAt,
                                     Map<String, String> parameters) {
    }

    public record RegisterFileRequest(@NotBlank String fileName, @NotBlank String contentType, long sizeBytes,
                                      @NotBlank String checksumSha256) {
    }

    public record SetConfigurationRequest(@NotBlank String key, @NotBlank String value, boolean encrypted,
                                          @NotBlank String actor) {
    }

    public record SetFeatureFlagRequest(@NotBlank String key, boolean enabled, String description,
                                        @NotBlank String actor) {
    }

    public record SetLocalizationRequest(@NotBlank String locale, @NotBlank String messageKey,
                                         @NotBlank String message) {
    }

    public record OutboxResponse(UUID id, String eventType, String sourceContext, String status, Instant createdAt) {
        static OutboxResponse from(final OutboxMessage message) {
            return new OutboxResponse(message.id(), message.event().eventType(), message.event().sourceContext(),
                    message.status().name(), message.createdAt());
        }
    }

    public record JobResponse(UUID id, String jobType, String status, Instant scheduledAt) {
        static JobResponse from(final BackgroundJob job) {
            return new JobResponse(job.id(), job.jobType(), job.status().name(), job.scheduledAt());
        }
    }

    public record ErrorCatalogResponse(String code, String httpStatus, String title, String ownerContext) {
        static ErrorCatalogResponse from(final ErrorCatalogEntry entry) {
            return new ErrorCatalogResponse(entry.code(), entry.httpStatus(), entry.title(), entry.ownerContext());
        }
    }

    public record EventCatalogResponse(String eventType, String ownerContext, String description) {
        static EventCatalogResponse from(final DomainEventCatalogEntry entry) {
            return new EventCatalogResponse(entry.eventType(), entry.ownerContext(), entry.description());
        }
    }

    private PlatformDtos() {
    }
}
