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
import com.newland.erp.platform.domain.StoredFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlatformRepository {
    OutboxMessage insertOutboxMessage(OutboxMessage message);

    List<OutboxMessage> listPendingOutboxMessages(int limit);

    AuditRecord insertAuditRecord(AuditRecord record);

    List<AuditRecord> listAuditRecords(String targetType, UUID targetId);

    BackgroundJob insertBackgroundJob(BackgroundJob job);

    List<BackgroundJob> listJobs();

    StoredFile insertStoredFile(StoredFile file);

    Optional<StoredFile> findStoredFile(UUID id);

    Attachment insertAttachment(Attachment attachment);

    List<Attachment> listAttachments(String ownerContext, String ownerType, UUID ownerId);

    ConfigurationEntry upsertConfiguration(ConfigurationEntry entry);

    Optional<ConfigurationEntry> findConfiguration(String key);

    FeatureFlag upsertFeatureFlag(FeatureFlag flag);

    Optional<FeatureFlag> findFeatureFlag(String key);

    LocalizationMessage upsertLocalizationMessage(LocalizationMessage message);

    Optional<LocalizationMessage> findLocalizationMessage(String locale, String messageKey);

    List<ErrorCatalogEntry> listErrorCatalog();

    List<DomainEventCatalogEntry> listDomainEventCatalog();
}
