package com.newland.erp.masterdata.application;

import com.newland.erp.masterdata.domain.MasterDataRecord;
import com.newland.erp.masterdata.domain.MasterDataType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MasterDataRepository {
    MasterDataRecord insert(MasterDataRecord record);

    MasterDataRecord update(MasterDataRecord record);

    Optional<MasterDataRecord> findById(UUID id);

    Optional<MasterDataRecord> findByTypeAndCode(MasterDataType type, String code);

    List<MasterDataRecord> listByType(MasterDataType type);
}
