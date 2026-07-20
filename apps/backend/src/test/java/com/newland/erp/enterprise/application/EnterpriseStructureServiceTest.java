package com.newland.erp.enterprise.application;

import com.newland.erp.enterprise.application.EnterpriseStructureCommands.CreateEnterprise;
import com.newland.erp.enterprise.application.EnterpriseStructureCommands.CreateLegalEntity;
import com.newland.erp.enterprise.application.EnterpriseStructureCommands.UpdateEnterprise;
import com.newland.erp.enterprise.domain.Branch;
import com.newland.erp.enterprise.domain.BranchCode;
import com.newland.erp.enterprise.domain.Company;
import com.newland.erp.enterprise.domain.CompanyCode;
import com.newland.erp.enterprise.domain.CountryCode;
import com.newland.erp.enterprise.domain.CurrencyCode;
import com.newland.erp.enterprise.domain.DisplayName;
import com.newland.erp.enterprise.domain.DuplicateBusinessCodeException;
import com.newland.erp.enterprise.domain.Enterprise;
import com.newland.erp.enterprise.domain.EnterpriseCode;
import com.newland.erp.enterprise.domain.InactiveParentException;
import com.newland.erp.enterprise.domain.LegalEntity;
import com.newland.erp.enterprise.domain.LegalEntityCode;
import com.newland.erp.enterprise.domain.LifecycleStatus;
import com.newland.erp.enterprise.domain.LocalizedName;
import com.newland.erp.enterprise.domain.LocationCode;
import com.newland.erp.enterprise.domain.OptimisticLockConflictException;
import com.newland.erp.enterprise.domain.Warehouse;
import com.newland.erp.enterprise.domain.WarehouseCode;
import com.newland.erp.enterprise.domain.WarehouseLocation;
import com.newland.erp.enterprise.domain.WarehouseZone;
import com.newland.erp.enterprise.domain.ZoneCode;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

final class EnterpriseStructureServiceTest {
    private static final RequestMetadata METADATA = new RequestMetadata("tester", UUID.randomUUID());
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-20T00:00:00Z"), ZoneOffset.UTC);

    private final RecordingAuditPort audit = new RecordingAuditPort();
    private final RecordingEventPublisher events = new RecordingEventPublisher();
    private final FakeRepository repository = new FakeRepository();
    private final EnterpriseStructureService service = new EnterpriseStructureService(
            repository,
            permission -> {
            },
            audit,
            events,
            CLOCK
    );

    @Test
    void createsEnterpriseAndPublishesAuditAndApplicationEvent() {
        final Enterprise created = service.createEnterprise(new CreateEnterprise(new EnterpriseCode("nl"),
                new DisplayName("Newland"), new LocalizedName(Map.of("en", "Newland"))), METADATA);

        assertThat(created.code().value()).isEqualTo("NL");
        assertThat(created.status()).isEqualTo(LifecycleStatus.DRAFT);
        assertThat(events.events).hasSize(1);
        assertThat(audit.events).hasSize(1);
    }

    @Test
    void updatesEnterpriseAndPublishesAuditAndApplicationEvent() {
        final Enterprise created = service.createEnterprise(new CreateEnterprise(new EnterpriseCode("nl"),
                new DisplayName("Newland"), new LocalizedName(Map.of("en", "Newland"))), METADATA);
        events.events.clear();
        audit.events.clear();

        final Enterprise updated = service.updateEnterprise(new UpdateEnterprise(created.id(),
                new DisplayName("Newland Holdings"), new LocalizedName(Map.of("en", "Newland Holdings")),
                created.audit().version()), METADATA);

        assertThat(updated.audit().version()).isEqualTo(1L);
        assertThat(events.events).hasSize(1);
        assertThat(((EnterpriseStructureEvent) events.events.get(0)).eventType()).isEqualTo("EnterpriseUpdated");
        assertThat(audit.events).hasSize(1);
        assertThat(audit.events.get(0).eventType()).isEqualTo("EnterpriseUpdated");
    }

    @Test
    void rejectsDuplicateEnterpriseCode() {
        service.createEnterprise(new CreateEnterprise(new EnterpriseCode("nl"), new DisplayName("Newland"),
                new LocalizedName(Map.of())), METADATA);

        assertThatThrownBy(() -> service.createEnterprise(new CreateEnterprise(new EnterpriseCode("NL"),
                new DisplayName("Duplicate"), new LocalizedName(Map.of())), METADATA))
                .isInstanceOf(DuplicateBusinessCodeException.class);
    }

    @Test
    void rejectsLegalEntityCreationWhenEnterpriseIsNotActive() {
        final Enterprise enterprise = service.createEnterprise(new CreateEnterprise(new EnterpriseCode("nl"),
                new DisplayName("Newland"), new LocalizedName(Map.of())), METADATA);

        assertThatThrownBy(() -> service.createLegalEntity(new CreateLegalEntity(enterprise.id(),
                new LegalEntityCode("LE"), new DisplayName("Legal"), new LocalizedName(Map.of()),
                new CountryCode("US"), new CurrencyCode("USD")), METADATA))
                .isInstanceOf(InactiveParentException.class);
    }

    @Test
    void enforcesOptimisticVersionOnUpdate() {
        final Enterprise enterprise = service.createEnterprise(new CreateEnterprise(new EnterpriseCode("nl"),
                new DisplayName("Newland"), new LocalizedName(Map.of())), METADATA);

        assertThatThrownBy(() -> service.updateEnterprise(new UpdateEnterprise(enterprise.id(),
                new DisplayName("Changed"), new LocalizedName(Map.of()), 99L), METADATA))
                .isInstanceOf(OptimisticLockConflictException.class);
    }

    private static final class RecordingAuditPort implements AuditPort {
        private final List<EnterpriseStructureAuditEvent> events = new ArrayList<>();

        @Override
        public void record(final EnterpriseStructureAuditEvent event) {
            events.add(event);
        }
    }

    private static final class RecordingEventPublisher implements ApplicationEventPublisher {
        private final List<Object> events = new ArrayList<>();

        @Override
        public void publishEvent(final Object event) {
            events.add(event);
        }
    }

    private static final class FakeRepository implements EnterpriseStructureRepository {
        private final Map<UUID, Enterprise> enterprises = new HashMap<>();

        @Override
        public Enterprise insertEnterprise(final Enterprise enterprise) {
            enterprises.put(enterprise.id(), enterprise);
            return enterprise;
        }

        @Override
        public Enterprise updateEnterprise(final Enterprise enterprise, final long expectedVersion) {
            final Enterprise current = enterprises.get(enterprise.id());
            if (current == null || current.audit().version() != expectedVersion) {
                throw new OptimisticLockConflictException("Optimistic lock conflict for enterprise.");
            }
            enterprises.put(enterprise.id(), enterprise);
            return enterprise;
        }

        @Override
        public Optional<Enterprise> findEnterprise(final UUID id) {
            return Optional.ofNullable(enterprises.get(id));
        }

        @Override
        public List<Enterprise> listEnterprises() {
            return List.copyOf(enterprises.values());
        }

        @Override
        public boolean enterpriseCodeExists(final EnterpriseCode code) {
            return enterprises.values().stream().anyMatch(enterprise -> enterprise.code().equals(code));
        }

        @Override
        public LegalEntity insertLegalEntity(final LegalEntity legalEntity) {
            throw unsupported();
        }

        @Override
        public LegalEntity updateLegalEntity(final LegalEntity legalEntity, final long expectedVersion) {
            throw unsupported();
        }

        @Override
        public Optional<LegalEntity> findLegalEntity(final UUID id) {
            return Optional.empty();
        }

        @Override
        public List<LegalEntity> listLegalEntitiesByEnterprise(final UUID enterpriseId) {
            return List.of();
        }

        @Override
        public boolean legalEntityCodeExists(final UUID enterpriseId, final LegalEntityCode code) {
            return false;
        }

        @Override
        public long activeLegalEntityCount(final UUID enterpriseId) {
            return 0L;
        }

        @Override
        public Company insertCompany(final Company company) {
            throw unsupported();
        }

        @Override
        public Company updateCompany(final Company company, final long expectedVersion) {
            throw unsupported();
        }

        @Override
        public Optional<Company> findCompany(final UUID id) {
            return Optional.empty();
        }

        @Override
        public List<Company> listCompaniesByLegalEntity(final UUID legalEntityId) {
            return List.of();
        }

        @Override
        public boolean companyCodeExists(final UUID enterpriseId, final CompanyCode code) {
            return false;
        }

        @Override
        public long activeCompanyCount(final UUID legalEntityId) {
            return 0L;
        }

        @Override
        public Branch insertBranch(final Branch branch) {
            throw unsupported();
        }

        @Override
        public Branch updateBranch(final Branch branch, final long expectedVersion) {
            throw unsupported();
        }

        @Override
        public Optional<Branch> findBranch(final UUID id) {
            return Optional.empty();
        }

        @Override
        public List<Branch> listBranchesByCompany(final UUID companyId) {
            return List.of();
        }

        @Override
        public boolean branchCodeExists(final UUID companyId, final BranchCode code) {
            return false;
        }

        @Override
        public long activeBranchCount(final UUID companyId) {
            return 0L;
        }

        @Override
        public Warehouse insertWarehouse(final Warehouse warehouse) {
            throw unsupported();
        }

        @Override
        public Warehouse updateWarehouse(final Warehouse warehouse, final long expectedVersion) {
            throw unsupported();
        }

        @Override
        public Optional<Warehouse> findWarehouse(final UUID id) {
            return Optional.empty();
        }

        @Override
        public List<Warehouse> listWarehousesByCompany(final UUID companyId) {
            return List.of();
        }

        @Override
        public List<Warehouse> listWarehousesByBranch(final UUID branchId) {
            return List.of();
        }

        @Override
        public boolean warehouseCodeExists(final UUID companyId, final WarehouseCode code) {
            return false;
        }

        @Override
        public long activeWarehouseCountByCompany(final UUID companyId) {
            return 0L;
        }

        @Override
        public long activeWarehouseCountByBranch(final UUID branchId) {
            return 0L;
        }

        @Override
        public WarehouseZone insertZone(final WarehouseZone zone) {
            throw unsupported();
        }

        @Override
        public WarehouseZone updateZone(final WarehouseZone zone, final long expectedVersion) {
            throw unsupported();
        }

        @Override
        public Optional<WarehouseZone> findZone(final UUID id) {
            return Optional.empty();
        }

        @Override
        public List<WarehouseZone> listZonesByWarehouse(final UUID warehouseId) {
            return List.of();
        }

        @Override
        public boolean zoneCodeExists(final UUID warehouseId, final ZoneCode code) {
            return false;
        }

        @Override
        public long activeZoneCount(final UUID warehouseId) {
            return 0L;
        }

        @Override
        public WarehouseLocation insertLocation(final WarehouseLocation location) {
            throw unsupported();
        }

        @Override
        public WarehouseLocation updateLocation(final WarehouseLocation location, final long expectedVersion) {
            throw unsupported();
        }

        @Override
        public Optional<WarehouseLocation> findLocation(final UUID id) {
            return Optional.empty();
        }

        @Override
        public List<WarehouseLocation> listLocationsByZone(final UUID zoneId) {
            return List.of();
        }

        @Override
        public boolean locationCodeExists(final UUID zoneId, final LocationCode code) {
            return false;
        }

        @Override
        public long activeLocationCount(final UUID zoneId) {
            return 0L;
        }

        private static UnsupportedOperationException unsupported() {
            return new UnsupportedOperationException("Not needed by this service test.");
        }
    }
}
