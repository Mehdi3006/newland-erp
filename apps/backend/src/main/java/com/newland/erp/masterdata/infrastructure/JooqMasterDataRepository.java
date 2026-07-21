package com.newland.erp.masterdata.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.newland.erp.masterdata.application.MasterDataRepository;
import com.newland.erp.masterdata.domain.DuplicateMasterDataCodeException;
import com.newland.erp.masterdata.domain.MasterDataRecord;
import com.newland.erp.masterdata.domain.MasterDataType;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.JSONB;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public final class JooqMasterDataRepository implements MasterDataRepository {
    private static final TypeReference<Map<String, String>> STRING_MAP = new TypeReference<>() {
    };
    private final DSLContext dsl;
    private final ObjectMapper objectMapper;

    public JooqMasterDataRepository(final DSLContext dslContext, final ObjectMapper mapper) {
        this.dsl = dslContext;
        this.objectMapper = mapper;
    }

    @Override
    public MasterDataRecord insert(final MasterDataRecord record) {
        try {
            dsl.insertInto(table())
                    .columns(id(), text("aggregate_type"), text("code"), text("display_name"), uuid("parent_id"),
                            bool("active"), jsonb("attributes"), longField("version"), instant("created_at"),
                            instant("updated_at"))
                    .values(record.id(), record.type().name(), record.code(), record.name(), record.parentId(),
                            record.active(), json(record.attributes()), record.version(), record.createdAt(),
                            record.updatedAt())
                    .execute();
            return record;
        } catch (DataAccessException exception) {
            throw new DuplicateMasterDataCodeException("Master Data code already exists: " + record.code());
        }
    }

    @Override
    public MasterDataRecord update(final MasterDataRecord record) {
        dsl.update(table())
                .set(text("display_name"), record.name())
                .set(bool("active"), record.active())
                .set(jsonb("attributes"), json(record.attributes()))
                .set(longField("version"), record.version())
                .set(instant("updated_at"), record.updatedAt())
                .where(id().eq(record.id()))
                .execute();
        return record;
    }

    @Override
    public Optional<MasterDataRecord> findById(final UUID id) {
        return dsl.selectFrom(table()).where(id().eq(id)).fetchOptional(this::record);
    }

    @Override
    public Optional<MasterDataRecord> findByTypeAndCode(final MasterDataType type, final String code) {
        return dsl.selectFrom(table())
                .where(text("aggregate_type").eq(type.name()).and(text("code").eq(code.toUpperCase())))
                .fetchOptional(this::record);
    }

    @Override
    public List<MasterDataRecord> listByType(final MasterDataType type) {
        return dsl.selectFrom(table())
                .where(text("aggregate_type").eq(type.name()))
                .orderBy(text("code"))
                .fetch(this::record);
    }

    private MasterDataRecord record(final Record source) {
        return new MasterDataRecord(source.get(id()), MasterDataType.valueOf(source.get(text("aggregate_type"))),
                source.get(text("code")), source.get(text("display_name")), source.get(uuid("parent_id")),
                Boolean.TRUE.equals(source.get(bool("active"))), jsonMap(source, "attributes"),
                source.get(longField("version")), valueInstant(source, "created_at"),
                valueInstant(source, "updated_at"));
    }

    private JSONB json(final Map<String, String> value) {
        try {
            return JSONB.valueOf(objectMapper.writeValueAsString(value == null ? Map.of() : value));
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Invalid Master Data attributes.", exception);
        }
    }

    private Map<String, String> jsonMap(final Record record, final String name) {
        try {
            return objectMapper.readValue(record.get(jsonb(name)).data(), STRING_MAP);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Invalid Master Data attributes.", exception);
        }
    }

    private static Table<Record> table() {
        return DSL.table(DSL.name("master_data_record"));
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

    private static Field<Boolean> bool(final String name) {
        return DSL.field(DSL.name(name), Boolean.class);
    }

    private static Field<Long> longField(final String name) {
        return DSL.field(DSL.name(name), Long.class);
    }

    private static Field<Instant> instant(final String name) {
        return DSL.field(DSL.name(name), Instant.class);
    }

    private static Field<JSONB> jsonb(final String name) {
        return DSL.field(DSL.name(name), JSONB.class);
    }

    private static Instant valueInstant(final Record record, final String name) {
        final Object value = record.get(DSL.field(DSL.name(name)));
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime.toInstant();
        }
        throw new IllegalStateException("Unsupported timestamp value for " + name + ".");
    }
}
