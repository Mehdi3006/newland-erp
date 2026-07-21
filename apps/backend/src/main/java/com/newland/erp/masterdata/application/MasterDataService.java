package com.newland.erp.masterdata.application;

import com.newland.erp.masterdata.domain.DuplicateMasterDataCodeException;
import com.newland.erp.masterdata.domain.MasterDataNotFoundException;
import com.newland.erp.masterdata.domain.MasterDataRecord;
import com.newland.erp.masterdata.domain.MasterDataType;
import com.newland.erp.masterdata.domain.MasterDataVersionConflictException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public final class MasterDataService {
    private final MasterDataRepository repository;
    private final Clock clock;

    public MasterDataService(final MasterDataRepository masterDataRepository, final Clock systemClock) {
        this.repository = masterDataRepository;
        this.clock = systemClock;
    }

    @Transactional
    public MasterDataRecord create(final MasterDataCommands.Create command) {
        repository.findByTypeAndCode(command.type(), command.code()).ifPresent(existing -> {
            throw new DuplicateMasterDataCodeException("Master Data code already exists: " + existing.code());
        });
        if (command.parentId() != null) {
            repository.findById(command.parentId()).orElseThrow(() ->
                    new MasterDataNotFoundException("Parent Master Data record not found: " + command.parentId()));
        }
        final Instant now = Instant.now(clock);
        return repository.insert(new MasterDataRecord(UUID.randomUUID(), command.type(), command.code(),
                command.name(), command.parentId(), true, command.attributes(), 0, now, now));
    }

    @Transactional
    public MasterDataRecord update(final MasterDataCommands.Update command) {
        final MasterDataRecord existing = get(command.id());
        if (existing.version() != command.expectedVersion()) {
            throw new MasterDataVersionConflictException("Master Data version conflict for: " + command.id());
        }
        return repository.update(existing.rename(command.name(), command.attributes(), Instant.now(clock)));
    }

    @Transactional
    public MasterDataRecord activate(final UUID id, final long expectedVersion) {
        final MasterDataRecord existing = getVersioned(id, expectedVersion);
        return repository.update(existing.activate(Instant.now(clock)));
    }

    @Transactional
    public MasterDataRecord deactivate(final UUID id, final long expectedVersion) {
        final MasterDataRecord existing = getVersioned(id, expectedVersion);
        return repository.update(existing.deactivate(Instant.now(clock)));
    }

    @Transactional(readOnly = true)
    public MasterDataRecord get(final UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new MasterDataNotFoundException("Master Data record not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<MasterDataRecord> list(final MasterDataType type) {
        return repository.listByType(type);
    }

    private MasterDataRecord getVersioned(final UUID id, final long expectedVersion) {
        final MasterDataRecord existing = get(id);
        if (existing.version() != expectedVersion) {
            throw new MasterDataVersionConflictException("Master Data version conflict for: " + id);
        }
        return existing;
    }
}
