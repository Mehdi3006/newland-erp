package com.newland.erp.enterprise.application;

import com.newland.erp.enterprise.application.EnterpriseStructureCommands.CreateBranch;
import com.newland.erp.enterprise.application.EnterpriseStructureCommands.CreateCompany;
import com.newland.erp.enterprise.application.EnterpriseStructureCommands.CreateEnterprise;
import com.newland.erp.enterprise.application.EnterpriseStructureCommands.CreateLegalEntity;
import com.newland.erp.enterprise.application.EnterpriseStructureCommands.CreateLocation;
import com.newland.erp.enterprise.application.EnterpriseStructureCommands.CreateWarehouse;
import com.newland.erp.enterprise.application.EnterpriseStructureCommands.CreateZone;
import com.newland.erp.enterprise.application.EnterpriseStructureCommands.UpdateBranch;
import com.newland.erp.enterprise.application.EnterpriseStructureCommands.UpdateCompany;
import com.newland.erp.enterprise.application.EnterpriseStructureCommands.UpdateEnterprise;
import com.newland.erp.enterprise.application.EnterpriseStructureCommands.UpdateLegalEntity;
import com.newland.erp.enterprise.application.EnterpriseStructureCommands.UpdateLocation;
import com.newland.erp.enterprise.application.EnterpriseStructureCommands.UpdateWarehouse;
import com.newland.erp.enterprise.application.EnterpriseStructureCommands.UpdateZone;
import com.newland.erp.enterprise.application.integration.EnterpriseReferencePort;
import com.newland.erp.enterprise.domain.AuditMetadata;
import com.newland.erp.enterprise.domain.Branch;
import com.newland.erp.enterprise.domain.Company;
import com.newland.erp.enterprise.domain.DuplicateBusinessCodeException;
import com.newland.erp.enterprise.domain.Enterprise;
import com.newland.erp.enterprise.domain.InactiveParentException;
import com.newland.erp.enterprise.domain.LegalEntity;
import com.newland.erp.enterprise.domain.LifecycleStatus;
import com.newland.erp.enterprise.domain.NotFoundException;
import com.newland.erp.enterprise.domain.ReferencedByActiveChildrenException;
import com.newland.erp.enterprise.domain.Warehouse;
import com.newland.erp.enterprise.domain.WarehouseLocation;
import com.newland.erp.enterprise.domain.WarehouseType;
import com.newland.erp.enterprise.domain.WarehouseZone;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public final class EnterpriseStructureService implements EnterpriseReferencePort {
    private final EnterpriseStructureRepository repository;
    private final AuthorizationPort authorization;
    private final AuditPort audit;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public EnterpriseStructureService(
            final EnterpriseStructureRepository repositoryPort,
            final AuthorizationPort authorizationPort,
            final AuditPort auditPort,
            final ApplicationEventPublisher eventPublisher,
            final Clock systemClock
    ) {
        this.repository = repositoryPort;
        this.authorization = authorizationPort;
        this.audit = auditPort;
        this.events = eventPublisher;
        this.clock = systemClock;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isActiveCompany(final UUID companyId) {
        return repository.findCompany(companyId)
                .map(company -> company.status() == LifecycleStatus.ACTIVE)
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isActiveBranch(final UUID companyId, final UUID branchId) {
        return repository.findBranch(branchId)
                .filter(branch -> branch.companyId().equals(companyId))
                .map(branch -> branch.status() == LifecycleStatus.ACTIVE)
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Optional<String> companyBaseCurrency(final UUID companyId) {
        return repository.findCompany(companyId)
                .filter(company -> company.status() == LifecycleStatus.ACTIVE)
                .map(company -> company.baseCurrency().value());
    }

    @Transactional
    public Enterprise createEnterprise(final CreateEnterprise command, final RequestMetadata metadata) {
        authorization.require(EnterpriseStructurePermissions.ENTERPRISE_MANAGE);
        if (repository.enterpriseCodeExists(command.code())) {
            throw new DuplicateBusinessCodeException("Enterprise code already exists: " + command.code().value());
        }
        final Enterprise enterprise = new Enterprise(UUID.randomUUID(), command.code(), command.name(),
                command.localizedName(), LifecycleStatus.DRAFT, AuditMetadata.created(now(), metadata.actor()));
        final Enterprise inserted = repository.insertEnterprise(enterprise);
        publish("EnterpriseCreated", inserted.id(), metadata, Map.of("code", inserted.code().value()));
        return inserted;
    }

    @Transactional
    public Enterprise updateEnterprise(final UpdateEnterprise command, final RequestMetadata metadata) {
        authorization.require(EnterpriseStructurePermissions.ENTERPRISE_MANAGE);
        final Enterprise enterprise = enterprise(command.id());
        final Enterprise updated = repository.updateEnterprise(enterprise.rename(command.name(),
                command.localizedName(), enterprise.audit().touched(now(), metadata.actor())),
                command.expectedVersion());
        publish("EnterpriseUpdated", updated.id(), metadata, Map.of("code", updated.code().value()));
        return updated;
    }

    @Transactional
    public Enterprise activateEnterprise(final UUID id, final RequestMetadata metadata) {
        authorization.require(EnterpriseStructurePermissions.ENTERPRISE_MANAGE);
        final Enterprise enterprise = enterprise(id);
        final Enterprise updated = repository.updateEnterprise(enterprise.activate(touch(enterprise, metadata)),
                enterprise.audit().version());
        publish("EnterpriseActivated", updated.id(), metadata, Map.of("code", updated.code().value()));
        return updated;
    }

    @Transactional
    public Enterprise deactivateEnterprise(final UUID id, final RequestMetadata metadata) {
        authorization.require(EnterpriseStructurePermissions.ENTERPRISE_MANAGE);
        if (repository.activeLegalEntityCount(id) > 0) {
            throw new ReferencedByActiveChildrenException("Enterprise has active legal entities.");
        }
        final Enterprise enterprise = enterprise(id);
        final Enterprise updated = repository.updateEnterprise(enterprise.deactivate(enterprise.audit().touched(now(),
                metadata.actor())), enterprise.audit().version());
        publish("EnterpriseDeactivated", updated.id(), metadata, Map.of("code", updated.code().value()));
        return updated;
    }

    @Transactional(readOnly = true)
    public Enterprise getEnterprise(final UUID id) {
        authorization.require(EnterpriseStructurePermissions.ENTERPRISE_READ);
        return enterprise(id);
    }

    @Transactional(readOnly = true)
    public List<Enterprise> listEnterprises() {
        authorization.require(EnterpriseStructurePermissions.ENTERPRISE_READ);
        return repository.listEnterprises();
    }

    @Transactional
    public LegalEntity createLegalEntity(final CreateLegalEntity command, final RequestMetadata metadata) {
        authorization.require(EnterpriseStructurePermissions.LEGAL_ENTITY_MANAGE);
        final Enterprise parent = enterprise(command.enterpriseId());
        requireActive(parent.status(), "enterprise");
        if (repository.legalEntityCodeExists(parent.id(), command.code())) {
            throw new DuplicateBusinessCodeException("Legal entity code already exists in enterprise.");
        }
        final LegalEntity legalEntity = new LegalEntity(UUID.randomUUID(), parent.id(), command.code(), command.name(),
                command.localizedName(), command.countryCode(), command.baseCurrency(), LifecycleStatus.DRAFT,
                AuditMetadata.created(now(), metadata.actor()));
        final LegalEntity inserted = repository.insertLegalEntity(legalEntity);
        publish("LegalEntityCreated", inserted.id(), metadata, Map.of("code", inserted.code().value()));
        return inserted;
    }

    @Transactional
    public LegalEntity updateLegalEntity(final UpdateLegalEntity command, final RequestMetadata metadata) {
        authorization.require(EnterpriseStructurePermissions.LEGAL_ENTITY_MANAGE);
        final LegalEntity legalEntity = legalEntity(command.id());
        final LegalEntity updated = repository.updateLegalEntity(legalEntity.update(command.name(),
                command.localizedName(), command.countryCode(), command.baseCurrency(),
                legalEntity.audit().touched(now(), metadata.actor())),
                command.expectedVersion());
        publish("LegalEntityUpdated", updated.id(), metadata, Map.of("code", updated.code().value()));
        return updated;
    }

    @Transactional
    public LegalEntity activateLegalEntity(final UUID id, final RequestMetadata metadata) {
        authorization.require(EnterpriseStructurePermissions.LEGAL_ENTITY_MANAGE);
        final LegalEntity legalEntity = legalEntity(id);
        requireActive(enterprise(legalEntity.enterpriseId()).status(), "enterprise");
        final LegalEntity updated = repository.updateLegalEntity(legalEntity.activate(legalEntity.audit().touched(now(),
                metadata.actor())), legalEntity.audit().version());
        publish("LegalEntityActivated", updated.id(), metadata, Map.of("code", updated.code().value()));
        return updated;
    }

    @Transactional
    public LegalEntity deactivateLegalEntity(final UUID id, final RequestMetadata metadata) {
        authorization.require(EnterpriseStructurePermissions.LEGAL_ENTITY_MANAGE);
        if (repository.activeCompanyCount(id) > 0) {
            throw new ReferencedByActiveChildrenException("Legal entity has active companies.");
        }
        final LegalEntity legalEntity = legalEntity(id);
        final LegalEntity updated = repository.updateLegalEntity(legalEntity.deactivate(legalEntity.audit().touched(
                now(), metadata.actor())), legalEntity.audit().version());
        publish("LegalEntityDeactivated", updated.id(), metadata, Map.of("code", updated.code().value()));
        return updated;
    }

    @Transactional(readOnly = true)
    public LegalEntity getLegalEntity(final UUID id) {
        authorization.require(EnterpriseStructurePermissions.LEGAL_ENTITY_READ);
        return legalEntity(id);
    }

    @Transactional(readOnly = true)
    public List<LegalEntity> listLegalEntitiesByEnterprise(final UUID enterpriseId) {
        authorization.require(EnterpriseStructurePermissions.LEGAL_ENTITY_READ);
        return repository.listLegalEntitiesByEnterprise(enterpriseId);
    }

    @Transactional
    public Company createCompany(final CreateCompany command, final RequestMetadata metadata) {
        authorization.require(EnterpriseStructurePermissions.COMPANY_MANAGE);
        final LegalEntity parent = legalEntity(command.legalEntityId());
        requireActive(parent.status(), "legal entity");
        if (repository.companyCodeExists(parent.enterpriseId(), command.code())) {
            throw new DuplicateBusinessCodeException("Company code already exists in enterprise.");
        }
        final Company company = new Company(UUID.randomUUID(), parent.enterpriseId(), parent.id(), command.code(),
                command.name(), command.localizedName(), command.countryCode(), command.baseCurrency(),
                command.timeZoneId(), command.address(), LifecycleStatus.DRAFT,
                AuditMetadata.created(now(), metadata.actor()));
        final Company inserted = repository.insertCompany(company);
        publish("CompanyCreated", inserted.id(), metadata, Map.of("code", inserted.code().value()));
        return inserted;
    }

    @Transactional
    public Company updateCompany(final UpdateCompany command, final RequestMetadata metadata) {
        authorization.require(EnterpriseStructurePermissions.COMPANY_MANAGE);
        final Company company = company(command.id());
        final Company updated = repository.updateCompany(company.update(command.name(), command.localizedName(),
                command.countryCode(), command.baseCurrency(), command.timeZoneId(), command.address(),
                company.audit().touched(now(), metadata.actor())), command.expectedVersion());
        publish("CompanyUpdated", updated.id(), metadata, Map.of("code", updated.code().value()));
        return updated;
    }

    @Transactional
    public Company activateCompany(final UUID id, final RequestMetadata metadata) {
        authorization.require(EnterpriseStructurePermissions.COMPANY_MANAGE);
        final Company company = company(id);
        requireActive(legalEntity(company.legalEntityId()).status(), "legal entity");
        final Company updated = repository.updateCompany(company.activate(company.audit().touched(now(),
                metadata.actor())), company.audit().version());
        publish("CompanyActivated", updated.id(), metadata, Map.of("code", updated.code().value()));
        return updated;
    }

    @Transactional
    public Company deactivateCompany(final UUID id, final RequestMetadata metadata) {
        authorization.require(EnterpriseStructurePermissions.COMPANY_MANAGE);
        if (repository.activeBranchCount(id) > 0 || repository.activeWarehouseCountByCompany(id) > 0) {
            throw new ReferencedByActiveChildrenException("Company has active branches or warehouses.");
        }
        final Company company = company(id);
        final Company updated = repository.updateCompany(company.deactivate(company.audit().touched(now(),
                metadata.actor())), company.audit().version());
        publish("CompanyDeactivated", updated.id(), metadata, Map.of("code", updated.code().value()));
        return updated;
    }

    @Transactional(readOnly = true)
    public Company getCompany(final UUID id) {
        authorization.require(EnterpriseStructurePermissions.COMPANY_READ);
        return company(id);
    }

    @Transactional(readOnly = true)
    public List<Company> listCompaniesByLegalEntity(final UUID legalEntityId) {
        authorization.require(EnterpriseStructurePermissions.COMPANY_READ);
        return repository.listCompaniesByLegalEntity(legalEntityId);
    }

    @Transactional
    public Branch createBranch(final CreateBranch command, final RequestMetadata metadata) {
        authorization.require(EnterpriseStructurePermissions.BRANCH_MANAGE);
        final Company parent = company(command.companyId());
        requireActive(parent.status(), "company");
        if (repository.branchCodeExists(parent.id(), command.code())) {
            throw new DuplicateBusinessCodeException("Branch code already exists in company.");
        }
        final Branch branch = new Branch(UUID.randomUUID(), parent.enterpriseId(), parent.id(), command.code(),
                command.name(), command.localizedName(), command.address(), LifecycleStatus.DRAFT,
                AuditMetadata.created(now(), metadata.actor()));
        final Branch inserted = repository.insertBranch(branch);
        publish("BranchCreated", inserted.id(), metadata, Map.of("code", inserted.code().value()));
        return inserted;
    }

    @Transactional
    public Branch updateBranch(final UpdateBranch command, final RequestMetadata metadata) {
        authorization.require(EnterpriseStructurePermissions.BRANCH_MANAGE);
        final Branch branch = branch(command.id());
        final Branch updated = repository.updateBranch(branch.update(command.name(), command.localizedName(),
                command.address(),
                branch.audit().touched(now(), metadata.actor())), command.expectedVersion());
        publish("BranchUpdated", updated.id(), metadata, Map.of("code", updated.code().value()));
        return updated;
    }

    @Transactional
    public Branch activateBranch(final UUID id, final RequestMetadata metadata) {
        authorization.require(EnterpriseStructurePermissions.BRANCH_MANAGE);
        final Branch branch = branch(id);
        requireActive(company(branch.companyId()).status(), "company");
        final Branch updated = repository.updateBranch(branch.activate(branch.audit().touched(now(), metadata.actor())),
                branch.audit().version());
        publish("BranchActivated", updated.id(), metadata, Map.of("code", updated.code().value()));
        return updated;
    }

    @Transactional
    public Branch deactivateBranch(final UUID id, final RequestMetadata metadata) {
        authorization.require(EnterpriseStructurePermissions.BRANCH_MANAGE);
        if (repository.activeWarehouseCountByBranch(id) > 0) {
            throw new ReferencedByActiveChildrenException("Branch has active warehouses.");
        }
        final Branch branch = branch(id);
        final Branch updated = repository.updateBranch(
                branch.deactivate(branch.audit().touched(now(), metadata.actor())),
                branch.audit().version()
        );
        publish("BranchDeactivated", updated.id(), metadata, Map.of("code", updated.code().value()));
        return updated;
    }

    @Transactional(readOnly = true)
    public Branch getBranch(final UUID id) {
        authorization.require(EnterpriseStructurePermissions.BRANCH_READ);
        return branch(id);
    }

    @Transactional(readOnly = true)
    public List<Branch> listBranchesByCompany(final UUID companyId) {
        authorization.require(EnterpriseStructurePermissions.BRANCH_READ);
        return repository.listBranchesByCompany(companyId);
    }

    @Transactional
    public Warehouse createWarehouse(final CreateWarehouse command, final RequestMetadata metadata) {
        authorization.require(EnterpriseStructurePermissions.WAREHOUSE_MANAGE);
        final Company company = company(command.companyId());
        requireActive(company.status(), "company");
        requireWarehouseParent(command, company);
        if (repository.warehouseCodeExists(company.id(), command.code())) {
            throw new DuplicateBusinessCodeException("Warehouse code already exists in company.");
        }
        final Warehouse warehouse = new Warehouse(UUID.randomUUID(), company.enterpriseId(), company.id(),
                command.branchId(), command.code(), command.name(), command.localizedName(), command.type(),
                command.projectReference(), command.address(), LifecycleStatus.DRAFT,
                AuditMetadata.created(now(), metadata.actor()));
        final Warehouse inserted = repository.insertWarehouse(warehouse);
        publish("WarehouseCreated", inserted.id(), metadata, Map.of("code", inserted.code().value()));
        return inserted;
    }

    @Transactional
    public Warehouse updateWarehouse(final UpdateWarehouse command, final RequestMetadata metadata) {
        authorization.require(EnterpriseStructurePermissions.WAREHOUSE_MANAGE);
        final Warehouse warehouse = warehouse(command.id());
        final Warehouse updated = repository.updateWarehouse(warehouse.update(command.name(), command.localizedName(),
                command.type(),
                command.projectReference(), command.address(), warehouse.audit().touched(now(), metadata.actor())),
                command.expectedVersion());
        publish("WarehouseUpdated", updated.id(), metadata, Map.of("code", updated.code().value()));
        return updated;
    }

    @Transactional
    public Warehouse activateWarehouse(final UUID id, final RequestMetadata metadata) {
        authorization.require(EnterpriseStructurePermissions.WAREHOUSE_MANAGE);
        final Warehouse warehouse = warehouse(id);
        requireActive(company(warehouse.companyId()).status(), "company");
        if (warehouse.branchId() != null) {
            requireActive(branch(warehouse.branchId()).status(), "branch");
        }
        final Warehouse updated = repository.updateWarehouse(warehouse.activate(warehouse.audit().touched(now(),
                metadata.actor())), warehouse.audit().version());
        publish("WarehouseActivated", updated.id(), metadata, Map.of("code", updated.code().value()));
        return updated;
    }

    @Transactional
    public Warehouse deactivateWarehouse(final UUID id, final RequestMetadata metadata) {
        authorization.require(EnterpriseStructurePermissions.WAREHOUSE_MANAGE);
        if (repository.activeZoneCount(id) > 0) {
            throw new ReferencedByActiveChildrenException("Warehouse has active zones.");
        }
        final Warehouse warehouse = warehouse(id);
        final Warehouse updated = repository.updateWarehouse(warehouse.deactivate(warehouse.audit().touched(now(),
                metadata.actor())), warehouse.audit().version());
        publish("WarehouseDeactivated", updated.id(), metadata, Map.of("code", updated.code().value()));
        return updated;
    }

    @Transactional(readOnly = true)
    public Warehouse getWarehouse(final UUID id) {
        authorization.require(EnterpriseStructurePermissions.WAREHOUSE_READ);
        return warehouse(id);
    }

    @Transactional(readOnly = true)
    public List<Warehouse> listWarehousesByCompany(final UUID companyId) {
        authorization.require(EnterpriseStructurePermissions.WAREHOUSE_READ);
        return repository.listWarehousesByCompany(companyId);
    }

    @Transactional(readOnly = true)
    public List<Warehouse> listWarehousesByBranch(final UUID branchId) {
        authorization.require(EnterpriseStructurePermissions.WAREHOUSE_READ);
        return repository.listWarehousesByBranch(branchId);
    }

    @Transactional
    public WarehouseZone createZone(final CreateZone command, final RequestMetadata metadata) {
        authorization.require(EnterpriseStructurePermissions.LOCATION_MANAGE);
        final Warehouse warehouse = warehouse(command.warehouseId());
        requireActive(warehouse.status(), "warehouse");
        if (repository.zoneCodeExists(warehouse.id(), command.code())) {
            throw new DuplicateBusinessCodeException("Zone code already exists in warehouse.");
        }
        final WarehouseZone zone = new WarehouseZone(UUID.randomUUID(), warehouse.enterpriseId(), warehouse.companyId(),
                warehouse.id(), command.code(), command.name(), command.localizedName(), LifecycleStatus.DRAFT,
                AuditMetadata.created(now(), metadata.actor()));
        final WarehouseZone inserted = repository.insertZone(zone);
        publish("WarehouseZoneCreated", inserted.id(), metadata, Map.of("code", inserted.code().value()));
        return inserted;
    }

    @Transactional
    public WarehouseZone updateZone(final UpdateZone command, final RequestMetadata metadata) {
        authorization.require(EnterpriseStructurePermissions.LOCATION_MANAGE);
        final WarehouseZone zone = zone(command.id());
        final WarehouseZone updated = repository.updateZone(zone.update(command.name(), command.localizedName(),
                zone.audit().touched(now(), metadata.actor())), command.expectedVersion());
        publish("WarehouseZoneUpdated", updated.id(), metadata, Map.of("code", updated.code().value()));
        return updated;
    }

    @Transactional
    public WarehouseZone activateZone(final UUID id, final RequestMetadata metadata) {
        authorization.require(EnterpriseStructurePermissions.LOCATION_MANAGE);
        final WarehouseZone zone = zone(id);
        requireActive(warehouse(zone.warehouseId()).status(), "warehouse");
        final WarehouseZone updated = repository.updateZone(zone.activate(zone.audit().touched(now(),
                metadata.actor())),
                zone.audit().version());
        publish("WarehouseZoneActivated", updated.id(), metadata, Map.of("code", updated.code().value()));
        return updated;
    }

    @Transactional
    public WarehouseZone deactivateZone(final UUID id, final RequestMetadata metadata) {
        authorization.require(EnterpriseStructurePermissions.LOCATION_MANAGE);
        if (repository.activeLocationCount(id) > 0) {
            throw new ReferencedByActiveChildrenException("Zone has active locations.");
        }
        final WarehouseZone zone = zone(id);
        final WarehouseZone updated = repository.updateZone(zone.deactivate(zone.audit().touched(now(),
                metadata.actor())),
                zone.audit().version());
        publish("WarehouseZoneDeactivated", updated.id(), metadata, Map.of("code", updated.code().value()));
        return updated;
    }

    @Transactional(readOnly = true)
    public WarehouseZone getZone(final UUID id) {
        authorization.require(EnterpriseStructurePermissions.LOCATION_READ);
        return zone(id);
    }

    @Transactional(readOnly = true)
    public List<WarehouseZone> listZonesByWarehouse(final UUID warehouseId) {
        authorization.require(EnterpriseStructurePermissions.LOCATION_READ);
        return repository.listZonesByWarehouse(warehouseId);
    }

    @Transactional
    public WarehouseLocation createLocation(final CreateLocation command, final RequestMetadata metadata) {
        authorization.require(EnterpriseStructurePermissions.LOCATION_MANAGE);
        final WarehouseZone zone = zone(command.zoneId());
        requireActive(zone.status(), "zone");
        if (repository.locationCodeExists(zone.id(), command.code())) {
            throw new DuplicateBusinessCodeException("Location code already exists in zone.");
        }
        final WarehouseLocation location = new WarehouseLocation(UUID.randomUUID(), zone.enterpriseId(),
                zone.companyId(), zone.warehouseId(), zone.id(), command.code(), command.name(),
                command.localizedName(), LifecycleStatus.DRAFT, AuditMetadata.created(now(), metadata.actor()));
        final WarehouseLocation inserted = repository.insertLocation(location);
        publish("WarehouseLocationCreated", inserted.id(), metadata, Map.of("code", inserted.code().value()));
        return inserted;
    }

    @Transactional
    public WarehouseLocation updateLocation(final UpdateLocation command, final RequestMetadata metadata) {
        authorization.require(EnterpriseStructurePermissions.LOCATION_MANAGE);
        final WarehouseLocation location = location(command.id());
        final WarehouseLocation updated = repository.updateLocation(location.update(command.name(),
                command.localizedName(), location.audit().touched(now(), metadata.actor())),
                command.expectedVersion());
        publish("WarehouseLocationUpdated", updated.id(), metadata, Map.of("code", updated.code().value()));
        return updated;
    }

    @Transactional
    public WarehouseLocation activateLocation(final UUID id, final RequestMetadata metadata) {
        authorization.require(EnterpriseStructurePermissions.LOCATION_MANAGE);
        final WarehouseLocation location = location(id);
        requireActive(zone(location.zoneId()).status(), "zone");
        final WarehouseLocation updated = repository.updateLocation(location.activate(location.audit().touched(now(),
                metadata.actor())),
                location.audit().version());
        publish("WarehouseLocationActivated", updated.id(), metadata, Map.of("code", updated.code().value()));
        return updated;
    }

    @Transactional
    public WarehouseLocation deactivateLocation(final UUID id, final RequestMetadata metadata) {
        authorization.require(EnterpriseStructurePermissions.LOCATION_MANAGE);
        final WarehouseLocation location = location(id);
        final WarehouseLocation updated = repository.updateLocation(location.deactivate(location.audit().touched(now(),
                metadata.actor())),
                location.audit().version());
        publish("WarehouseLocationDeactivated", updated.id(), metadata, Map.of("code", updated.code().value()));
        return updated;
    }

    @Transactional(readOnly = true)
    public WarehouseLocation getLocation(final UUID id) {
        authorization.require(EnterpriseStructurePermissions.LOCATION_READ);
        return location(id);
    }

    @Transactional(readOnly = true)
    public List<WarehouseLocation> listLocationsByZone(final UUID zoneId) {
        authorization.require(EnterpriseStructurePermissions.LOCATION_READ);
        return repository.listLocationsByZone(zoneId);
    }

    private Instant now() {
        return Instant.now(clock);
    }

    private AuditMetadata touch(final Enterprise enterprise, final RequestMetadata metadata) {
        return enterprise.audit().touched(now(), metadata.actor());
    }

    private void publish(
            final String eventType,
            final UUID aggregateId,
            final RequestMetadata metadata,
            final Map<String, String> payload
    ) {
        final EnterpriseStructureEvent event = new EnterpriseStructureEvent(UUID.randomUUID(), eventType, aggregateId,
                now(), metadata.actor(), metadata.correlationId(), Map.copyOf(payload));
        events.publishEvent(event);
        audit.record(new EnterpriseStructureAuditEvent(event.eventId(), event.eventType(), event.aggregateId(),
                event.actor(), event.correlationId(), event.occurredAt()));
    }

    private Enterprise enterprise(final UUID id) {
        return repository.findEnterprise(id).orElseThrow(() -> new NotFoundException("Enterprise not found: " + id));
    }

    private LegalEntity legalEntity(final UUID id) {
        return repository.findLegalEntity(id).orElseThrow(() -> new NotFoundException("Legal entity not found: " + id));
    }

    private Company company(final UUID id) {
        return repository.findCompany(id).orElseThrow(() -> new NotFoundException("Company not found: " + id));
    }

    private Branch branch(final UUID id) {
        return repository.findBranch(id).orElseThrow(() -> new NotFoundException("Branch not found: " + id));
    }

    private Warehouse warehouse(final UUID id) {
        return repository.findWarehouse(id).orElseThrow(() -> new NotFoundException("Warehouse not found: " + id));
    }

    private WarehouseZone zone(final UUID id) {
        return repository.findZone(id).orElseThrow(() -> new NotFoundException("Warehouse zone not found: " + id));
    }

    private WarehouseLocation location(final UUID id) {
        return repository.findLocation(id).orElseThrow(() -> new NotFoundException("Warehouse location not found: "
                + id));
    }

    private void requireActive(final LifecycleStatus status, final String parentName) {
        if (status != LifecycleStatus.ACTIVE) {
            throw new InactiveParentException("Cannot use inactive parent: " + parentName);
        }
    }

    private void requireWarehouseParent(final CreateWarehouse command, final Company company) {
        if (command.branchId() != null) {
            final Branch branch = branch(command.branchId());
            if (!branch.companyId().equals(company.id())) {
                throw new InactiveParentException("Warehouse branch must belong to the same company.");
            }
            requireActive(branch.status(), "branch");
        }
        if (command.type() == WarehouseType.BRANCH && command.branchId() == null) {
            throw new InactiveParentException("Branch warehouse requires an active branch parent.");
        }
    }
}
