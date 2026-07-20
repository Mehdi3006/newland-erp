package com.newland.erp.platform.infrastructure;

import com.newland.erp.platform.application.SchedulerPort;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public final class InMemorySchedulerAdapter implements SchedulerPort {
    private final ConcurrentMap<UUID, ScheduledJob> scheduledJobs = new ConcurrentHashMap<>();

    @Override
    public void schedule(final UUID jobId, final String jobType, final Instant scheduledAt,
                         final Map<String, String> parameters) {
        scheduledJobs.put(jobId, new ScheduledJob(jobType, scheduledAt, Map.copyOf(parameters)));
    }

    public boolean contains(final UUID jobId) {
        return scheduledJobs.containsKey(jobId);
    }

    private record ScheduledJob(String jobType, Instant scheduledAt, Map<String, String> parameters) {
    }
}
