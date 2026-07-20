package com.newland.erp.platform.domain;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record BackgroundJob(UUID id, String jobType, JobStatus status, Instant scheduledAt, Instant startedAt,
                            Instant completedAt, Map<String, String> parameters, String lastError) {
    public BackgroundJob {
        PlatformDomainEvent.require(id, "job id");
        PlatformDomainEvent.requireText(jobType, "job type");
        PlatformDomainEvent.require(status, "job status");
        PlatformDomainEvent.require(scheduledAt, "scheduled at");
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
    }
}
