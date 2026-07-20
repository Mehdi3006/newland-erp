package com.newland.erp.platform.application;

import com.newland.erp.platform.domain.Attachment;
import com.newland.erp.platform.domain.AuditRecord;
import com.newland.erp.platform.domain.BackgroundJob;
import com.newland.erp.platform.domain.ConfigurationEntry;
import com.newland.erp.platform.domain.DomainEventCatalogEntry;
import com.newland.erp.platform.domain.ErrorCatalogEntry;
import com.newland.erp.platform.domain.FeatureFlag;
import com.newland.erp.platform.domain.LocalizationMessage;
import com.newland.erp.platform.domain.OutboxMessage;
import com.newland.erp.platform.domain.OutboxStatus;
import com.newland.erp.platform.domain.StoredFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

final class InMemoryPlatformRepository implements PlatformRepository {
    private final List<OutboxMessage> outbox = new ArrayList<>();
    private final List<BackgroundJob> jobs = new ArrayList<>();
    private final List<StoredFile> files = new ArrayList<>();

    @Override
    public OutboxMessage insertOutboxMessage(final OutboxMessage message) {
        outbox.add(message);
        return message;
    }

    @Override
    public List<OutboxMessage> listPendingOutboxMessages(final int limit) {
        return outbox.stream().filter(message -> message.status() == OutboxStatus.PENDING).limit(limit).toList();
    }

    @Override
    public AuditRecord insertAuditRecord(final AuditRecord record) {
        return record;
    }

    @Override
    public List<AuditRecord> listAuditRecords(final String targetType, final UUID targetId) {
        return List.of();
    }

    @Override
    public BackgroundJob insertBackgroundJob(final BackgroundJob job) {
        jobs.add(job);
        return job;
    }

    @Override
    public List<BackgroundJob> listJobs() {
        return List.copyOf(jobs);
    }

    @Override
    public StoredFile insertStoredFile(final StoredFile file) {
        files.add(file);
        return file;
    }

    @Override
    public Optional<StoredFile> findStoredFile(final UUID id) {
        return files.stream().filter(file -> file.id().equals(id)).findFirst();
    }

    @Override
    public Attachment insertAttachment(final Attachment attachment) {
        return attachment;
    }

    @Override
    public List<Attachment> listAttachments(final String ownerContext, final String ownerType, final UUID ownerId) {
        return List.of();
    }

    @Override
    public ConfigurationEntry upsertConfiguration(final ConfigurationEntry entry) {
        return entry;
    }

    @Override
    public Optional<ConfigurationEntry> findConfiguration(final String key) {
        return Optional.empty();
    }

    @Override
    public FeatureFlag upsertFeatureFlag(final FeatureFlag flag) {
        return flag;
    }

    @Override
    public Optional<FeatureFlag> findFeatureFlag(final String key) {
        return Optional.empty();
    }

    @Override
    public LocalizationMessage upsertLocalizationMessage(final LocalizationMessage message) {
        return message;
    }

    @Override
    public Optional<LocalizationMessage> findLocalizationMessage(final String locale, final String messageKey) {
        return Optional.empty();
    }

    @Override
    public List<ErrorCatalogEntry> listErrorCatalog() {
        return List.of();
    }

    @Override
    public List<DomainEventCatalogEntry> listDomainEventCatalog() {
        return List.of();
    }
}
