package com.newland.erp.platform.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.newland.erp.platform.application.PlatformRepository;
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
import com.newland.erp.platform.domain.OutboxStatus;
import com.newland.erp.platform.domain.PlatformDomainEvent;
import com.newland.erp.platform.domain.StoredFile;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.JSONB;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public final class JooqPlatformRepository implements PlatformRepository {
    private static final TypeReference<Map<String, String>> STRING_MAP = new TypeReference<>() {
    };
    private final DSLContext dsl;
    private final ObjectMapper objectMapper;

    public JooqPlatformRepository(final DSLContext dslContext, final ObjectMapper mapper) {
        this.dsl = dslContext;
        this.objectMapper = mapper;
    }

    @Override
    public OutboxMessage insertOutboxMessage(final OutboxMessage message) {
        dsl.insertInto(table("platform_outbox"))
                .columns(id(), uuid("event_id"), text("source_context"), text("event_type"), uuid("aggregate_id"),
                        instant("occurred_at"), jsonb("payload"), text("status"), integer("attempts"),
                        instant("next_attempt_at"), instant("created_at"), instant("published_at"),
                        text("last_error"))
                .values(message.id(), message.event().eventId(), message.event().sourceContext(),
                        message.event().eventType(), message.event().aggregateId(), message.event().occurredAt(),
                        json(message.event().payload()), message.status().name(), message.attempts(),
                        message.nextAttemptAt(), message.createdAt(), message.publishedAt(), message.lastError())
                .execute();
        return message;
    }

    @Override
    public List<OutboxMessage> listPendingOutboxMessages(final int limit) {
        return dsl.selectFrom(table("platform_outbox"))
                .where(text("status").in(OutboxStatus.PENDING.name(), OutboxStatus.FAILED.name()))
                .orderBy(instant("next_attempt_at"))
                .limit(limit)
                .fetch(this::outboxMessage);
    }

    @Override
    public List<OutboxMessage> claimOutboxMessages(final int limit, final Instant now,
                                                   final Instant leaseExpiresAt) {
        return dsl.resultQuery("""
                WITH candidates AS (
                    SELECT id
                      FROM platform_outbox
                     WHERE status IN ('PENDING', 'FAILED', 'PROCESSING')
                       AND next_attempt_at <= CAST(? AS timestamptz)
                     ORDER BY next_attempt_at, created_at
                     LIMIT ?
                     FOR UPDATE SKIP LOCKED
                )
                UPDATE platform_outbox AS message
                   SET status = 'PROCESSING',
                       attempts = message.attempts + 1,
                       next_attempt_at = CAST(? AS timestamptz),
                       last_error = NULL
                  FROM candidates
                 WHERE message.id = candidates.id
                RETURNING message.*
                """, now, limit, leaseExpiresAt)
                .fetch(this::outboxMessage);
    }

    @Override
    public void markOutboxPublished(final UUID messageId, final int attempts,
                                    final Instant publishedAt) {
        final int updated = dsl.update(table("platform_outbox"))
                .set(text("status"), OutboxStatus.PUBLISHED.name())
                .set(instant("published_at"), publishedAt)
                .set(text("last_error"), (String) null)
                .where(uuid("id").eq(messageId)
                        .and(text("status").eq(OutboxStatus.PROCESSING.name()))
                        .and(integer("attempts").eq(attempts)))
                .execute();
        requireOutboxTransition(updated);
    }

    @Override
    public void markOutboxFailed(final UUID messageId, final int attempts,
                                 final Instant nextAttemptAt, final String lastError) {
        final int updated = dsl.update(table("platform_outbox"))
                .set(text("status"), OutboxStatus.FAILED.name())
                .set(instant("next_attempt_at"), nextAttemptAt)
                .set(text("last_error"), lastError)
                .where(uuid("id").eq(messageId)
                        .and(text("status").eq(OutboxStatus.PROCESSING.name()))
                        .and(integer("attempts").eq(attempts)))
                .execute();
        requireOutboxTransition(updated);
    }

    @Override
    public AuditRecord insertAuditRecord(final AuditRecord record) {
        dsl.insertInto(table("platform_audit_log"))
                .columns(id(), text("actor"), text("action"), text("target_type"), uuid("target_id"),
                        instant("occurred_at"), jsonb("attributes"))
                .values(record.id(), record.actor(), record.action(), record.targetType(), record.targetId(),
                        record.occurredAt(), json(record.attributes()))
                .execute();
        return record;
    }

    @Override
    public List<AuditRecord> listAuditRecords(final String targetType, final UUID targetId) {
        return dsl.selectFrom(table("platform_audit_log"))
                .where(text("target_type").eq(targetType).and(uuid("target_id").eq(targetId)))
                .fetch(this::auditRecord);
    }

    @Override
    public BackgroundJob insertBackgroundJob(final BackgroundJob job) {
        dsl.insertInto(table("platform_background_job"))
                .columns(id(), text("job_type"), text("status"), instant("scheduled_at"), instant("started_at"),
                        instant("completed_at"), jsonb("parameters"), text("last_error"))
                .values(job.id(), job.jobType(), job.status().name(), job.scheduledAt(), job.startedAt(),
                        job.completedAt(), json(job.parameters()), job.lastError())
                .execute();
        return job;
    }

    @Override
    public List<BackgroundJob> listJobs() {
        return dsl.selectFrom(table("platform_background_job")).orderBy(instant("scheduled_at")).fetch(this::job);
    }

    @Override
    public StoredFile insertStoredFile(final StoredFile file) {
        dsl.insertInto(table("platform_stored_file"))
                .columns(id(), text("storage_key"), text("file_name"), text("content_type"), longField("size_bytes"),
                        text("checksum_sha256"), instant("created_at"))
                .values(file.id(), file.storageKey(), file.fileName(), file.contentType(), file.sizeBytes(),
                        file.checksumSha256(), file.createdAt())
                .execute();
        return file;
    }

    @Override
    public Optional<StoredFile> findStoredFile(final UUID id) {
        return dsl.selectFrom(table("platform_stored_file")).where(id().eq(id)).fetchOptional(this::storedFile);
    }

    @Override
    public Attachment insertAttachment(final Attachment attachment) {
        dsl.insertInto(table("platform_attachment"))
                .columns(id(), text("owner_context"), text("owner_type"), uuid("owner_id"), uuid("file_id"),
                        instant("attached_at"))
                .values(attachment.id(), attachment.ownerContext(), attachment.ownerType(), attachment.ownerId(),
                        attachment.fileId(), attachment.attachedAt())
                .execute();
        return attachment;
    }

    @Override
    public List<Attachment> listAttachments(final String ownerContext, final String ownerType, final UUID ownerId) {
        return dsl.selectFrom(table("platform_attachment"))
                .where(text("owner_context").eq(ownerContext).and(text("owner_type").eq(ownerType))
                        .and(uuid("owner_id").eq(ownerId)))
                .fetch(this::attachment);
    }

    @Override
    public ConfigurationEntry upsertConfiguration(final ConfigurationEntry entry) {
        dsl.insertInto(table("platform_configuration"))
                .columns(text("config_key"), text("config_value"), bool("encrypted"), instant("updated_at"),
                        text("updated_by"))
                .values(entry.key(), entry.value(), entry.encrypted(), entry.updatedAt(), entry.updatedBy())
                .onDuplicateKeyUpdate()
                .set(text("config_value"), entry.value())
                .set(bool("encrypted"), entry.encrypted())
                .set(instant("updated_at"), entry.updatedAt())
                .set(text("updated_by"), entry.updatedBy())
                .execute();
        return entry;
    }

    @Override
    public Optional<ConfigurationEntry> findConfiguration(final String key) {
        return dsl.selectFrom(table("platform_configuration"))
                .where(text("config_key").eq(key))
                .fetchOptional(record -> new ConfigurationEntry(record.get(text("config_key")),
                        record.get(text("config_value")), Boolean.TRUE.equals(record.get(bool("encrypted"))),
                        auditInstant(record, "updated_at"), record.get(text("updated_by"))));
    }

    @Override
    public FeatureFlag upsertFeatureFlag(final FeatureFlag flag) {
        dsl.insertInto(table("platform_feature_flag"))
                .columns(text("flag_key"), bool("enabled"), text("description"), instant("updated_at"),
                        text("updated_by"))
                .values(flag.key(), flag.enabled(), flag.description(), flag.updatedAt(), flag.updatedBy())
                .onDuplicateKeyUpdate()
                .set(bool("enabled"), flag.enabled())
                .set(text("description"), flag.description())
                .set(instant("updated_at"), flag.updatedAt())
                .set(text("updated_by"), flag.updatedBy())
                .execute();
        return flag;
    }

    @Override
    public Optional<FeatureFlag> findFeatureFlag(final String key) {
        return dsl.selectFrom(table("platform_feature_flag"))
                .where(text("flag_key").eq(key))
                .fetchOptional(record -> new FeatureFlag(record.get(text("flag_key")),
                        Boolean.TRUE.equals(record.get(bool("enabled"))), record.get(text("description")),
                        auditInstant(record, "updated_at"), record.get(text("updated_by"))));
    }

    @Override
    public LocalizationMessage upsertLocalizationMessage(final LocalizationMessage message) {
        dsl.insertInto(table("platform_localization_message"))
                .columns(text("locale"), text("message_key"), text("message"))
                .values(message.locale(), message.messageKey(), message.message())
                .onDuplicateKeyUpdate()
                .set(text("message"), message.message())
                .execute();
        return message;
    }

    @Override
    public Optional<LocalizationMessage> findLocalizationMessage(final String locale, final String messageKey) {
        return dsl.selectFrom(table("platform_localization_message"))
                .where(text("locale").eq(locale).and(text("message_key").eq(messageKey)))
                .fetchOptional(record -> new LocalizationMessage(record.get(text("locale")),
                        record.get(text("message_key")), record.get(text("message"))));
    }

    @Override
    public List<ErrorCatalogEntry> listErrorCatalog() {
        return dsl.selectFrom(table("platform_error_catalog"))
                .orderBy(text("code"))
                .fetch(record -> new ErrorCatalogEntry(record.get(text("code")), record.get(text("http_status")),
                        record.get(text("title")), record.get(text("owner_context"))));
    }

    @Override
    public List<DomainEventCatalogEntry> listDomainEventCatalog() {
        return dsl.selectFrom(table("platform_domain_event_catalog"))
                .orderBy(text("event_type"))
                .fetch(record -> new DomainEventCatalogEntry(record.get(text("event_type")),
                        record.get(text("owner_context")), record.get(text("description"))));
    }

    private OutboxMessage outboxMessage(final Record record) {
        final PlatformDomainEvent event = new PlatformDomainEvent(record.get(uuid("event_id")),
                record.get(text("source_context")), record.get(text("event_type")), record.get(uuid("aggregate_id")),
                auditInstant(record, "occurred_at"), jsonMap(record, "payload"));
        return new OutboxMessage(record.get(id()), event, OutboxStatus.valueOf(record.get(text("status"))),
                record.get(integer("attempts")), auditInstant(record, "next_attempt_at"),
                auditInstant(record, "created_at"), auditInstant(record, "published_at"),
                record.get(text("last_error")));
    }

    private static void requireOutboxTransition(final int updated) {
        if (updated != 1) {
            throw new IllegalStateException("Outbox claim is no longer owned by this dispatcher.");
        }
    }

    private AuditRecord auditRecord(final Record record) {
        return new AuditRecord(record.get(id()), record.get(text("actor")), record.get(text("action")),
                record.get(text("target_type")), record.get(uuid("target_id")), auditInstant(record, "occurred_at"),
                jsonMap(record, "attributes"));
    }

    private BackgroundJob job(final Record record) {
        return new BackgroundJob(record.get(id()), record.get(text("job_type")),
                JobStatus.valueOf(record.get(text("status"))), auditInstant(record, "scheduled_at"),
                auditInstant(record, "started_at"), auditInstant(record, "completed_at"),
                jsonMap(record, "parameters"), record.get(text("last_error")));
    }

    private StoredFile storedFile(final Record record) {
        return new StoredFile(record.get(id()), record.get(text("storage_key")), record.get(text("file_name")),
                record.get(text("content_type")), record.get(longField("size_bytes")),
                record.get(text("checksum_sha256")), auditInstant(record, "created_at"));
    }

    private Attachment attachment(final Record record) {
        return new Attachment(record.get(id()), record.get(text("owner_context")), record.get(text("owner_type")),
                record.get(uuid("owner_id")), record.get(uuid("file_id")), auditInstant(record, "attached_at"));
    }

    private JSONB json(final Map<String, String> value) {
        try {
            return JSONB.valueOf(objectMapper.writeValueAsString(value == null ? Map.of() : value));
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Invalid JSON payload.", exception);
        }
    }

    private Map<String, String> jsonMap(final Record record, final String name) {
        try {
            return objectMapper.readValue(record.get(jsonb(name)).data(), STRING_MAP);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Invalid JSON payload.", exception);
        }
    }

    private static Table<Record> table(final String name) {
        return DSL.table(DSL.name(name));
    }

    private static Field<UUID> id() {
        return uuid("id");
    }

    private static Field<UUID> uuid(final String name) {
        return DSL.field(DSL.name(name), UUID.class);
    }

    private static Field<String> text(final String name) {
        return DSL.field(DSL.name(name), String.class);
    }

    private static Field<Integer> integer(final String name) {
        return DSL.field(DSL.name(name), Integer.class);
    }

    private static Field<Long> longField(final String name) {
        return DSL.field(DSL.name(name), Long.class);
    }

    private static Field<Boolean> bool(final String name) {
        return DSL.field(DSL.name(name), Boolean.class);
    }

    private static Field<Instant> instant(final String name) {
        return DSL.field(DSL.name(name), Instant.class);
    }

    private static Field<JSONB> jsonb(final String name) {
        return DSL.field(DSL.name(name), JSONB.class);
    }

    private static Instant auditInstant(final Record record, final String name) {
        final Object value = record.get(DSL.field(DSL.name(name)));
        if (value == null) {
            return null;
        }
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime.toInstant();
        }
        throw new IllegalStateException("Unsupported timestamp value for " + name + ": "
                + value.getClass().getName());
    }
}
