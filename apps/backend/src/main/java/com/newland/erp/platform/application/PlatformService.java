package com.newland.erp.platform.application;

import com.newland.erp.platform.domain.Attachment;
import com.newland.erp.platform.domain.AuditRecord;
import com.newland.erp.platform.domain.BackgroundJob;
import com.newland.erp.platform.domain.ConfigurationEntry;
import com.newland.erp.platform.domain.DomainEventCatalogEntry;
import com.newland.erp.platform.domain.ErrorCatalogEntry;
import com.newland.erp.platform.domain.FeatureFlag;
import com.newland.erp.platform.domain.JobStatus;
import com.newland.erp.platform.domain.LocalizationMessage;
import com.newland.erp.platform.domain.OutboxMessage;
import com.newland.erp.platform.domain.PlatformDomainEvent;
import com.newland.erp.platform.domain.PlatformNotFoundException;
import com.newland.erp.platform.domain.StoredFile;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public final class PlatformService {
    private final PlatformRepository repository;
    private final DomainEventBus eventBus;
    private final Clock clock;

    public PlatformService(final PlatformRepository platformRepository, final DomainEventBus bus,
                           final Clock systemClock) {
        this.repository = platformRepository;
        this.eventBus = bus;
        this.clock = systemClock;
    }

    @Transactional
    public OutboxMessage publishEvent(final PlatformCommands.PublishEvent command) {
        final PlatformDomainEvent event = new PlatformDomainEvent(UUID.randomUUID(), command.sourceContext(),
                command.eventType(), command.aggregateId(), now(), command.payload());
        eventBus.publish(event);
        return repository.insertOutboxMessage(OutboxMessage.pending(event, now()));
    }

    @Transactional
    public AuditRecord recordAudit(final PlatformCommands.RecordAudit command) {
        return repository.insertAuditRecord(new AuditRecord(UUID.randomUUID(), command.actor(), command.action(),
                command.targetType(), command.targetId(), now(), command.attributes()));
    }

    @Transactional
    public BackgroundJob scheduleJob(final PlatformCommands.ScheduleJob command) {
        return repository.insertBackgroundJob(new BackgroundJob(UUID.randomUUID(), command.jobType(),
                JobStatus.SCHEDULED, command.scheduledAt(), null, null, command.parameters(), null));
    }

    @Transactional
    public StoredFile registerFile(final PlatformCommands.RegisterStoredFile command) {
        final String key = "platform/" + UUID.randomUUID();
        return repository.insertStoredFile(new StoredFile(UUID.randomUUID(), key, command.fileName(),
                command.contentType(), command.sizeBytes(), command.checksumSha256(), now()));
    }

    @Transactional
    public Attachment attachFile(final PlatformCommands.AttachFile command) {
        repository.findStoredFile(command.fileId())
                .orElseThrow(() -> new PlatformNotFoundException("Stored file not found: " + command.fileId()));
        return repository.insertAttachment(new Attachment(UUID.randomUUID(), command.ownerContext(),
                command.ownerType(), command.ownerId(), command.fileId(), now()));
    }

    @Transactional
    public ConfigurationEntry setConfiguration(final PlatformCommands.SetConfiguration command) {
        return repository.upsertConfiguration(new ConfigurationEntry(command.key(), command.value(),
                command.encrypted(), now(), command.actor()));
    }

    @Transactional
    public FeatureFlag setFeatureFlag(final PlatformCommands.SetFeatureFlag command) {
        return repository.upsertFeatureFlag(new FeatureFlag(command.key(), command.enabled(), command.description(),
                now(), command.actor()));
    }

    @Transactional
    public LocalizationMessage setLocalization(final PlatformCommands.SetLocalization command) {
        return repository.upsertLocalizationMessage(new LocalizationMessage(command.locale(), command.messageKey(),
                command.message()));
    }

    @Transactional(readOnly = true)
    public List<OutboxMessage> pendingOutbox(final int limit) {
        return repository.listPendingOutboxMessages(limit);
    }

    @Transactional(readOnly = true)
    public List<BackgroundJob> jobs() {
        return repository.listJobs();
    }

    @Transactional(readOnly = true)
    public Map<String, Boolean> featureFlag(final String key) {
        return Map.of(key, repository.findFeatureFlag(key).map(FeatureFlag::enabled).orElse(false));
    }

    @Transactional(readOnly = true)
    public List<ErrorCatalogEntry> errorCatalog() {
        return repository.listErrorCatalog();
    }

    @Transactional(readOnly = true)
    public List<DomainEventCatalogEntry> domainEventCatalog() {
        return repository.listDomainEventCatalog();
    }

    private Instant now() {
        return Instant.now(clock);
    }
}
