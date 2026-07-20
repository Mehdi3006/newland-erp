package com.newland.erp.platform.application;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public interface SchedulerPort {
    void schedule(UUID jobId, String jobType, Instant scheduledAt, Map<String, String> parameters);
}
