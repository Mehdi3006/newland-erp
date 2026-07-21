package com.newland.erp.masterdata.domain;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record MasterDataRecord(
        UUID id,
        MasterDataType type,
        String code,
        String name,
        UUID parentId,
        boolean active,
        Map<String, String> attributes,
        long version,
        Instant createdAt,
        Instant updatedAt
) {
    public MasterDataRecord {
        if (id == null) {
            throw new IllegalArgumentException("Master Data id is required.");
        }
        if (type == null) {
            throw new IllegalArgumentException("Master Data type is required.");
        }
        code = required("code", code).toUpperCase();
        name = required("name", name);
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        if (version < 0) {
            throw new IllegalArgumentException("Master Data version cannot be negative.");
        }
        if (createdAt == null || updatedAt == null) {
            throw new IllegalArgumentException("Master Data timestamps are required.");
        }
    }

    public MasterDataRecord rename(final String newName, final Map<String, String> newAttributes,
                                   final Instant changedAt) {
        return new MasterDataRecord(id, type, code, newName, parentId, active, newAttributes, version + 1,
                createdAt, changedAt);
    }

    public MasterDataRecord activate(final Instant changedAt) {
        return new MasterDataRecord(id, type, code, name, parentId, true, attributes, version + 1, createdAt,
                changedAt);
    }

    public MasterDataRecord deactivate(final Instant changedAt) {
        return new MasterDataRecord(id, type, code, name, parentId, false, attributes, version + 1, createdAt,
                changedAt);
    }

    private static String required(final String name, final String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Master Data " + name + " is required.");
        }
        return value.trim();
    }
}
