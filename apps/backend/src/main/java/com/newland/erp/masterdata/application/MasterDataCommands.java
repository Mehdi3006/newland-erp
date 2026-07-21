package com.newland.erp.masterdata.application;

import com.newland.erp.masterdata.domain.MasterDataType;

import java.util.Map;
import java.util.UUID;

public final class MasterDataCommands {
    public record Create(MasterDataType type, String code, String name, UUID parentId,
                         Map<String, String> attributes) {
    }

    public record Update(UUID id, String name, Map<String, String> attributes, long expectedVersion) {
    }

    private MasterDataCommands() {
    }
}
