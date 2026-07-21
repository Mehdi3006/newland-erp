package com.newland.erp.masterdata.application;

import com.newland.erp.masterdata.domain.MasterDataRecord;
import com.newland.erp.masterdata.domain.MasterDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

final class InMemoryMasterDataRepository implements MasterDataRepository {
    private final List<MasterDataRecord> records = new ArrayList<>();

    @Override
    public MasterDataRecord insert(final MasterDataRecord record) {
        records.add(record);
        return record;
    }

    @Override
    public MasterDataRecord update(final MasterDataRecord record) {
        records.removeIf(existing -> existing.id().equals(record.id()));
        records.add(record);
        return record;
    }

    @Override
    public Optional<MasterDataRecord> findById(final UUID id) {
        return records.stream().filter(record -> record.id().equals(id)).findFirst();
    }

    @Override
    public Optional<MasterDataRecord> findByTypeAndCode(final MasterDataType type, final String code) {
        return records.stream()
                .filter(record -> record.type() == type && record.code().equals(code.toUpperCase()))
                .findFirst();
    }

    @Override
    public List<MasterDataRecord> listByType(final MasterDataType type) {
        return records.stream().filter(record -> record.type() == type).toList();
    }
}
