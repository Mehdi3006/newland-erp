package com.newland.erp.masterdata.api;

import com.newland.erp.masterdata.domain.MasterDataRecord;
import com.newland.erp.masterdata.domain.MasterDataType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public final class MasterDataDtos {
    public record CreateRequest(@NotBlank String code, @NotBlank String name, UUID parentId,
                                Map<String, String> attributes) {
    }

    public record UpdateRequest(@NotBlank String name, Map<String, String> attributes,
                                @PositiveOrZero long expectedVersion) {
    }

    public record LifecycleRequest(@PositiveOrZero long expectedVersion) {
    }

    public record TypeResponse(String slug, String name) {
        static TypeResponse from(final MasterDataType type) {
            return new TypeResponse(type.slug(), type.name());
        }
    }

    public record MasterDataResponse(@NotNull UUID id, String type, String code, String name, UUID parentId,
                                     boolean active, Map<String, String> attributes, long version,
                                     Instant createdAt, Instant updatedAt) {
        static MasterDataResponse from(final MasterDataRecord record) {
            return new MasterDataResponse(record.id(), record.type().slug(), record.code(), record.name(),
                    record.parentId(), record.active(), record.attributes(), record.version(), record.createdAt(),
                    record.updatedAt());
        }
    }

    private MasterDataDtos() {
    }
}
