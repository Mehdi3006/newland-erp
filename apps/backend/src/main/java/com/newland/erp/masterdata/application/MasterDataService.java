package com.newland.erp.masterdata.application;

import com.newland.erp.masterdata.domain.DuplicateMasterDataCodeException;
import com.newland.erp.masterdata.domain.MasterDataNotFoundException;
import com.newland.erp.masterdata.domain.MasterDataRecord;
import com.newland.erp.masterdata.domain.MasterDataType;
import com.newland.erp.masterdata.domain.MasterDataVersionConflictException;
import com.newland.erp.masterdata.application.integration.MasterDataReferencePort;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public final class MasterDataService implements MasterDataReferencePort {
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

    @Override
    @Transactional(readOnly = true)
    public boolean isActiveCurrency(final String currencyCode) {
        if (currencyCode == null || currencyCode.isBlank()) {
            return false;
        }
        return repository.findByTypeAndCode(MasterDataType.CURRENCY, currencyCode)
                .map(MasterDataRecord::active)
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isActiveReference(final String referenceType, final String referenceCode) {
        if (referenceType == null || referenceType.isBlank()
                || referenceCode == null || referenceCode.isBlank()) {
            return false;
        }
        final MasterDataType type;
        try {
            type = MasterDataType.fromSlug(referenceType);
        } catch (IllegalArgumentException exception) {
            return false;
        }
        return repository.findByTypeAndCode(type, referenceCode)
                .map(MasterDataRecord::active)
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Optional<ExchangeRateSnapshot> resolveExchangeRate(
            final UUID companyId, final String sourceCurrency, final String targetCurrency,
            final java.time.LocalDate effectiveDate) {
        if (companyId == null || sourceCurrency == null || targetCurrency == null
                || effectiveDate == null) {
            return java.util.Optional.empty();
        }
        return repository.listByType(MasterDataType.EXCHANGE_RATE).stream()
                .filter(MasterDataRecord::active)
                .map(this::exchangeRate)
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::orElseThrow)
                .filter(rate -> rate.companyId().equals(companyId)
                        && rate.sourceCurrency().equalsIgnoreCase(sourceCurrency)
                        && rate.targetCurrency().equalsIgnoreCase(targetCurrency)
                        && !effectiveDate.isBefore(rate.validFrom())
                        && (rate.validTo() == null || !effectiveDate.isAfter(rate.validTo())))
                .findFirst();
    }

    private java.util.Optional<ExchangeRateSnapshot> exchangeRate(final MasterDataRecord record) {
        final Map<String, String> attributes = record.attributes();
        try {
            final String validTo = attributes.get("validTo");
            return java.util.Optional.of(new ExchangeRateSnapshot(
                    record.id(),
                    UUID.fromString(attributes.get("companyId")),
                    attributes.get("sourceCurrency").toUpperCase(java.util.Locale.ROOT),
                    attributes.get("targetCurrency").toUpperCase(java.util.Locale.ROOT),
                    java.time.LocalDate.parse(attributes.get("validFrom")),
                    validTo == null || validTo.isBlank() ? null : java.time.LocalDate.parse(validTo),
                    new java.math.BigDecimal(attributes.get("rate"))));
        } catch (RuntimeException exception) {
            return java.util.Optional.empty();
        }
    }

    private MasterDataRecord getVersioned(final UUID id, final long expectedVersion) {
        final MasterDataRecord existing = get(id);
        if (existing.version() != expectedVersion) {
            throw new MasterDataVersionConflictException("Master Data version conflict for: " + id);
        }
        return existing;
    }
}
