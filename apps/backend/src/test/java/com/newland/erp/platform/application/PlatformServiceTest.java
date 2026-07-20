package com.newland.erp.platform.application;

import com.newland.erp.platform.domain.OutboxStatus;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

final class PlatformServiceTest {
    @Test
    void publishesEventThroughBusAndPersistsOutboxMessage() {
        final InMemoryPlatformRepository repository = new InMemoryPlatformRepository();
        final AtomicInteger published = new AtomicInteger();
        final PlatformService service = new PlatformService(repository, event -> published.incrementAndGet(),
                new RecordingFileStorage(new HashSet<>()), (jobId, jobType, scheduledAt, parameters) -> {
                },
                Clock.fixed(Instant.parse("2026-07-20T00:00:00Z"), ZoneOffset.UTC));

        final var message = service.publishEvent(new PlatformCommands.PublishEvent("identity", "UserCreated",
                UUID.randomUUID(), Map.of("username", "owner")));

        assertThat(published).hasValue(1);
        assertThat(message.status()).isEqualTo(OutboxStatus.PENDING);
        assertThat(repository.listPendingOutboxMessages(10)).containsExactly(message);
    }

    @Test
    void storesFileThroughStoragePortBeforePersistingMetadata() {
        final InMemoryPlatformRepository repository = new InMemoryPlatformRepository();
        final Set<String> storedKeys = new HashSet<>();
        final PlatformService service = new PlatformService(repository, event -> {
        }, new RecordingFileStorage(storedKeys), (jobId, jobType, scheduledAt, parameters) -> {
        }, Clock.fixed(Instant.parse("2026-07-20T00:00:00Z"), ZoneOffset.UTC));

        final var file = service.storeFile(new PlatformCommands.StoreFile("policy.txt", "text/plain",
                "content".getBytes(), "0".repeat(64)));

        assertThat(storedKeys).containsExactly(file.storageKey());
        assertThat(repository.findStoredFile(file.id())).contains(file);
    }

    @Test
    void schedulesJobThroughSchedulerPortAfterPersistingJob() {
        final InMemoryPlatformRepository repository = new InMemoryPlatformRepository();
        final Set<UUID> scheduledJobs = new HashSet<>();
        final PlatformService service = new PlatformService(repository, event -> {
        }, new RecordingFileStorage(new HashSet<>()),
                (jobId, jobType, scheduledAt, parameters) -> scheduledJobs.add(jobId),
                Clock.fixed(Instant.parse("2026-07-20T00:00:00Z"), ZoneOffset.UTC));

        final var job = service.scheduleJob(new PlatformCommands.ScheduleJob("platform.outbox.dispatch",
                Instant.parse("2026-07-20T01:00:00Z"), Map.of("batchSize", "100")));

        assertThat(scheduledJobs).containsExactly(job.id());
        assertThat(repository.listJobs()).containsExactly(job);
    }

    private record RecordingFileStorage(Set<String> storedKeys) implements FileStoragePort {
        @Override
        public String put(final String storageKey, final byte[] content) {
            storedKeys.add(storageKey);
            return storageKey;
        }

        @Override
        public byte[] get(final String storageKey) {
            return new byte[0];
        }
    }
}
