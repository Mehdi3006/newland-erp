package com.newland.erp.enterprise.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.newland.erp.enterprise.application.EnterpriseStructureRepository;
import com.newland.erp.enterprise.domain.Address;
import com.newland.erp.enterprise.domain.AuditMetadata;
import com.newland.erp.enterprise.domain.Branch;
import com.newland.erp.enterprise.domain.BranchCode;
import com.newland.erp.enterprise.domain.Company;
import com.newland.erp.enterprise.domain.CompanyCode;
import com.newland.erp.enterprise.domain.CountryCode;
import com.newland.erp.enterprise.domain.CurrencyCode;
import com.newland.erp.enterprise.domain.DisplayName;
import com.newland.erp.enterprise.domain.Enterprise;
import com.newland.erp.enterprise.domain.EnterpriseCode;
import com.newland.erp.enterprise.domain.LegalEntity;
import com.newland.erp.enterprise.domain.LegalEntityCode;
import com.newland.erp.enterprise.domain.LifecycleStatus;
import com.newland.erp.enterprise.domain.LocalizedName;
import com.newland.erp.enterprise.domain.LocationCode;
import com.newland.erp.enterprise.domain.OptimisticLockConflictException;
import com.newland.erp.enterprise.domain.TimeZoneId;
import com.newland.erp.enterprise.domain.Warehouse;
import com.newland.erp.enterprise.domain.WarehouseCode;
import com.newland.erp.enterprise.domain.WarehouseLocation;
import com.newland.erp.enterprise.domain.WarehouseType;
import com.newland.erp.enterprise.domain.WarehouseZone;
import com.newland.erp.enterprise.domain.ZoneCode;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.JSONB;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public final class JooqEnterpriseStructureRepository implements EnterpriseStructureRepository {
    private static final TypeReference<Map<String, String>> LOCALIZED_NAME_TYPE = new TypeReference<>() {
    };

    private final DSLContext dsl;
    private final ObjectMapper objectMapper;

    public JooqEnterpriseStructureRepository(final DSLContext dsl, final ObjectMapper objectMapper) {
        this.dsl = dsl;
        this.objectMapper = objectMapper;
    }

    @Override
    public Enterprise insertEnterprise(final Enterprise enterprise) {
        dsl.insertInto(table("enterprise"))
                .columns(id(), text("code"), text("name"), jsonb("localized_name"), text("status"),
                        instant("created_at"), text("created_by"), instant("updated_at"), text("updated_by"),
                        version())
                .values(enterprise.id(), enterprise.code().value(), enterprise.name().value(), json(enterprise),
                        enterprise.status().name(), enterprise.audit().createdAt(), enterprise.audit().createdBy(),
                        enterprise.audit().updatedAt(), enterprise.audit().updatedBy(), enterprise.audit().version())
                .execute();
        return enterprise;
    }

    @Override
    public Enterprise updateEnterprise(final Enterprise enterprise, final long expectedVersion) {
        final int updated = dsl.update(table("enterprise"))
                .set(text("name"), enterprise.name().value())
                .set(jsonb("localized_name"), json(enterprise))
                .set(text("status"), enterprise.status().name())
                .set(instant("updated_at"), enterprise.audit().updatedAt())
                .set(text("updated_by"), enterprise.audit().updatedBy())
                .set(version(), enterprise.audit().version())
                .where(id().eq(enterprise.id()).and(version().eq(expectedVersion)))
                .execute();
        requireUpdated(updated, "enterprise", enterprise.id());
        return enterprise;
    }

    @Override
    public Optional<Enterprise> findEnterprise(final UUID id) {
        return dsl.selectFrom(table("enterprise")).where(id().eq(id)).fetchOptional(this::enterprise);
    }

    @Override
    public List<Enterprise> listEnterprises() {
        return dsl.selectFrom(table("enterprise")).orderBy(text("code")).fetch(this::enterprise);
    }

    @Override
    public boolean enterpriseCodeExists(final EnterpriseCode code) {
        return exists("enterprise", text("code").eq(code.value()));
    }

    @Override
    public LegalEntity insertLegalEntity(final LegalEntity legalEntity) {
        dsl.insertInto(table("legal_entity"))
                .columns(id(), uuid("enterprise_id"), text("code"), text("name"), jsonb("localized_name"),
                        text("country_code"), text("base_currency"), text("status"), instant("created_at"),
                        text("created_by"), instant("updated_at"), text("updated_by"), version())
                .values(legalEntity.id(), legalEntity.enterpriseId(), legalEntity.code().value(),
                        legalEntity.name().value(), json(legalEntity), legalEntity.countryCode().value(),
                        legalEntity.baseCurrency().value(), legalEntity.status().name(),
                        legalEntity.audit().createdAt(), legalEntity.audit().createdBy(),
                        legalEntity.audit().updatedAt(), legalEntity.audit().updatedBy(),
                        legalEntity.audit().version())
                .execute();
        return legalEntity;
    }

    @Override
    public LegalEntity updateLegalEntity(final LegalEntity legalEntity, final long expectedVersion) {
        final int updated = dsl.update(table("legal_entity"))
                .set(text("name"), legalEntity.name().value())
                .set(jsonb("localized_name"), json(legalEntity))
                .set(text("country_code"), legalEntity.countryCode().value())
                .set(text("base_currency"), legalEntity.baseCurrency().value())
                .set(text("status"), legalEntity.status().name())
                .set(instant("updated_at"), legalEntity.audit().updatedAt())
                .set(text("updated_by"), legalEntity.audit().updatedBy())
                .set(version(), legalEntity.audit().version())
                .where(id().eq(legalEntity.id()).and(version().eq(expectedVersion)))
                .execute();
        requireUpdated(updated, "legal_entity", legalEntity.id());
        return legalEntity;
    }

    @Override
    public Optional<LegalEntity> findLegalEntity(final UUID id) {
        return dsl.selectFrom(table("legal_entity")).where(id().eq(id)).fetchOptional(this::legalEntity);
    }

    @Override
    public List<LegalEntity> listLegalEntitiesByEnterprise(final UUID enterpriseId) {
        return dsl.selectFrom(table("legal_entity"))
                .where(uuid("enterprise_id").eq(enterpriseId))
                .orderBy(text("code"))
                .fetch(this::legalEntity);
    }

    @Override
    public boolean legalEntityCodeExists(final UUID enterpriseId, final LegalEntityCode code) {
        return exists("legal_entity", uuid("enterprise_id").eq(enterpriseId).and(text("code").eq(code.value())));
    }

    @Override
    public long activeLegalEntityCount(final UUID enterpriseId) {
        return activeCount("legal_entity", "enterprise_id", enterpriseId);
    }

    @Override
    public Company insertCompany(final Company company) {
        dsl.insertInto(table("company"))
                .columns(id(), uuid("enterprise_id"), uuid("legal_entity_id"), text("code"), text("name"),
                        jsonb("localized_name"), text("country_code"), text("base_currency"), text("time_zone_id"),
                        text("address_line1"), text("address_line2"), text("city"), text("region"),
                        text("postal_code"), text("status"), instant("created_at"), text("created_by"),
                        instant("updated_at"), text("updated_by"), version())
                .values(company.id(), company.enterpriseId(), company.legalEntityId(), company.code().value(),
                        company.name().value(), json(company), company.countryCode().value(),
                        company.baseCurrency().value(), company.timeZoneId().value(), line1(company.address()),
                        line2(company.address()), city(company.address()), region(company.address()),
                        postalCode(company.address()), company.status().name(), company.audit().createdAt(),
                        company.audit().createdBy(), company.audit().updatedAt(), company.audit().updatedBy(),
                        company.audit().version())
                .execute();
        return company;
    }

    @Override
    public Company updateCompany(final Company company, final long expectedVersion) {
        final int updated = dsl.update(table("company"))
                .set(text("name"), company.name().value())
                .set(jsonb("localized_name"), json(company))
                .set(text("country_code"), company.countryCode().value())
                .set(text("base_currency"), company.baseCurrency().value())
                .set(text("time_zone_id"), company.timeZoneId().value())
                .set(text("address_line1"), line1(company.address()))
                .set(text("address_line2"), line2(company.address()))
                .set(text("city"), city(company.address()))
                .set(text("region"), region(company.address()))
                .set(text("postal_code"), postalCode(company.address()))
                .set(text("status"), company.status().name())
                .set(instant("updated_at"), company.audit().updatedAt())
                .set(text("updated_by"), company.audit().updatedBy())
                .set(version(), company.audit().version())
                .where(id().eq(company.id()).and(version().eq(expectedVersion)))
                .execute();
        requireUpdated(updated, "company", company.id());
        return company;
    }

    @Override
    public Optional<Company> findCompany(final UUID id) {
        return dsl.selectFrom(table("company")).where(id().eq(id)).fetchOptional(this::company);
    }

    @Override
    public List<Company> listCompaniesByLegalEntity(final UUID legalEntityId) {
        return dsl.selectFrom(table("company"))
                .where(uuid("legal_entity_id").eq(legalEntityId))
                .orderBy(text("code"))
                .fetch(this::company);
    }

    @Override
    public boolean companyCodeExists(final UUID enterpriseId, final CompanyCode code) {
        return exists("company", uuid("enterprise_id").eq(enterpriseId).and(text("code").eq(code.value())));
    }

    @Override
    public long activeCompanyCount(final UUID legalEntityId) {
        return activeCount("company", "legal_entity_id", legalEntityId);
    }

    @Override
    public Branch insertBranch(final Branch branch) {
        dsl.insertInto(table("branch"))
                .columns(id(), uuid("enterprise_id"), uuid("company_id"), text("code"), text("name"),
                        jsonb("localized_name"), text("address_line1"), text("address_line2"), text("city"),
                        text("region"), text("postal_code"), text("status"), instant("created_at"),
                        text("created_by"), instant("updated_at"), text("updated_by"), version())
                .values(branch.id(), branch.enterpriseId(), branch.companyId(), branch.code().value(),
                        branch.name().value(), json(branch), line1(branch.address()), line2(branch.address()),
                        city(branch.address()), region(branch.address()), postalCode(branch.address()),
                        branch.status().name(), branch.audit().createdAt(), branch.audit().createdBy(),
                        branch.audit().updatedAt(), branch.audit().updatedBy(), branch.audit().version())
                .execute();
        return branch;
    }

    @Override
    public Branch updateBranch(final Branch branch, final long expectedVersion) {
        final int updated = dsl.update(table("branch"))
                .set(text("name"), branch.name().value())
                .set(jsonb("localized_name"), json(branch))
                .set(text("address_line1"), line1(branch.address()))
                .set(text("address_line2"), line2(branch.address()))
                .set(text("city"), city(branch.address()))
                .set(text("region"), region(branch.address()))
                .set(text("postal_code"), postalCode(branch.address()))
                .set(text("status"), branch.status().name())
                .set(instant("updated_at"), branch.audit().updatedAt())
                .set(text("updated_by"), branch.audit().updatedBy())
                .set(version(), branch.audit().version())
                .where(id().eq(branch.id()).and(version().eq(expectedVersion)))
                .execute();
        requireUpdated(updated, "branch", branch.id());
        return branch;
    }

    @Override
    public Optional<Branch> findBranch(final UUID id) {
        return dsl.selectFrom(table("branch")).where(id().eq(id)).fetchOptional(this::branch);
    }

    @Override
    public List<Branch> listBranchesByCompany(final UUID companyId) {
        return dsl.selectFrom(table("branch"))
                .where(uuid("company_id").eq(companyId))
                .orderBy(text("code"))
                .fetch(this::branch);
    }

    @Override
    public boolean branchCodeExists(final UUID companyId, final BranchCode code) {
        return exists("branch", uuid("company_id").eq(companyId).and(text("code").eq(code.value())));
    }

    @Override
    public long activeBranchCount(final UUID companyId) {
        return activeCount("branch", "company_id", companyId);
    }

    @Override
    public Warehouse insertWarehouse(final Warehouse warehouse) {
        dsl.insertInto(table("warehouse"))
                .columns(id(), uuid("enterprise_id"), uuid("company_id"), uuid("branch_id"), text("code"),
                        text("name"), jsonb("localized_name"), text("warehouse_type"), text("project_reference"),
                        text("address_line1"), text("address_line2"), text("city"), text("region"),
                        text("postal_code"), text("status"), instant("created_at"), text("created_by"),
                        instant("updated_at"), text("updated_by"), version())
                .values(warehouse.id(), warehouse.enterpriseId(), warehouse.companyId(), warehouse.branchId(),
                        warehouse.code().value(), warehouse.name().value(), json(warehouse),
                        warehouse.type().name(), warehouse.projectReference(), line1(warehouse.address()),
                        line2(warehouse.address()), city(warehouse.address()), region(warehouse.address()),
                        postalCode(warehouse.address()), warehouse.status().name(), warehouse.audit().createdAt(),
                        warehouse.audit().createdBy(), warehouse.audit().updatedAt(), warehouse.audit().updatedBy(),
                        warehouse.audit().version())
                .execute();
        return warehouse;
    }

    @Override
    public Warehouse updateWarehouse(final Warehouse warehouse, final long expectedVersion) {
        final int updated = dsl.update(table("warehouse"))
                .set(text("name"), warehouse.name().value())
                .set(jsonb("localized_name"), json(warehouse))
                .set(text("warehouse_type"), warehouse.type().name())
                .set(text("project_reference"), warehouse.projectReference())
                .set(text("address_line1"), line1(warehouse.address()))
                .set(text("address_line2"), line2(warehouse.address()))
                .set(text("city"), city(warehouse.address()))
                .set(text("region"), region(warehouse.address()))
                .set(text("postal_code"), postalCode(warehouse.address()))
                .set(text("status"), warehouse.status().name())
                .set(instant("updated_at"), warehouse.audit().updatedAt())
                .set(text("updated_by"), warehouse.audit().updatedBy())
                .set(version(), warehouse.audit().version())
                .where(id().eq(warehouse.id()).and(version().eq(expectedVersion)))
                .execute();
        requireUpdated(updated, "warehouse", warehouse.id());
        return warehouse;
    }

    @Override
    public Optional<Warehouse> findWarehouse(final UUID id) {
        return dsl.selectFrom(table("warehouse")).where(id().eq(id)).fetchOptional(this::warehouse);
    }

    @Override
    public List<Warehouse> listWarehousesByCompany(final UUID companyId) {
        return dsl.selectFrom(table("warehouse"))
                .where(uuid("company_id").eq(companyId))
                .orderBy(text("code"))
                .fetch(this::warehouse);
    }

    @Override
    public List<Warehouse> listWarehousesByBranch(final UUID branchId) {
        return dsl.selectFrom(table("warehouse"))
                .where(uuid("branch_id").eq(branchId))
                .orderBy(text("code"))
                .fetch(this::warehouse);
    }

    @Override
    public boolean warehouseCodeExists(final UUID companyId, final WarehouseCode code) {
        return exists("warehouse", uuid("company_id").eq(companyId).and(text("code").eq(code.value())));
    }

    @Override
    public long activeWarehouseCountByCompany(final UUID companyId) {
        return activeCount("warehouse", "company_id", companyId);
    }

    @Override
    public long activeWarehouseCountByBranch(final UUID branchId) {
        return activeCount("warehouse", "branch_id", branchId);
    }

    @Override
    public WarehouseZone insertZone(final WarehouseZone zone) {
        dsl.insertInto(table("warehouse_zone"))
                .columns(id(), uuid("enterprise_id"), uuid("company_id"), uuid("warehouse_id"), text("code"),
                        text("name"), jsonb("localized_name"), text("status"), instant("created_at"),
                        text("created_by"), instant("updated_at"), text("updated_by"), version())
                .values(zone.id(), zone.enterpriseId(), zone.companyId(), zone.warehouseId(), zone.code().value(),
                        zone.name().value(), json(zone), zone.status().name(), zone.audit().createdAt(),
                        zone.audit().createdBy(), zone.audit().updatedAt(), zone.audit().updatedBy(),
                        zone.audit().version())
                .execute();
        return zone;
    }

    @Override
    public WarehouseZone updateZone(final WarehouseZone zone, final long expectedVersion) {
        final int updated = dsl.update(table("warehouse_zone"))
                .set(text("name"), zone.name().value())
                .set(jsonb("localized_name"), json(zone))
                .set(text("status"), zone.status().name())
                .set(instant("updated_at"), zone.audit().updatedAt())
                .set(text("updated_by"), zone.audit().updatedBy())
                .set(version(), zone.audit().version())
                .where(id().eq(zone.id()).and(version().eq(expectedVersion)))
                .execute();
        requireUpdated(updated, "warehouse_zone", zone.id());
        return zone;
    }

    @Override
    public Optional<WarehouseZone> findZone(final UUID id) {
        return dsl.selectFrom(table("warehouse_zone")).where(id().eq(id)).fetchOptional(this::zone);
    }

    @Override
    public List<WarehouseZone> listZonesByWarehouse(final UUID warehouseId) {
        return dsl.selectFrom(table("warehouse_zone"))
                .where(uuid("warehouse_id").eq(warehouseId))
                .orderBy(text("code"))
                .fetch(this::zone);
    }

    @Override
    public boolean zoneCodeExists(final UUID warehouseId, final ZoneCode code) {
        return exists("warehouse_zone", uuid("warehouse_id").eq(warehouseId).and(text("code").eq(code.value())));
    }

    @Override
    public long activeZoneCount(final UUID warehouseId) {
        return activeCount("warehouse_zone", "warehouse_id", warehouseId);
    }

    @Override
    public WarehouseLocation insertLocation(final WarehouseLocation location) {
        dsl.insertInto(table("warehouse_location"))
                .columns(id(), uuid("enterprise_id"), uuid("company_id"), uuid("warehouse_id"), uuid("zone_id"),
                        text("code"), text("name"), jsonb("localized_name"), text("status"), instant("created_at"),
                        text("created_by"), instant("updated_at"), text("updated_by"), version())
                .values(location.id(), location.enterpriseId(), location.companyId(), location.warehouseId(),
                        location.zoneId(), location.code().value(), location.name().value(), json(location),
                        location.status().name(), location.audit().createdAt(), location.audit().createdBy(),
                        location.audit().updatedAt(), location.audit().updatedBy(), location.audit().version())
                .execute();
        return location;
    }

    @Override
    public WarehouseLocation updateLocation(final WarehouseLocation location, final long expectedVersion) {
        final int updated = dsl.update(table("warehouse_location"))
                .set(text("name"), location.name().value())
                .set(jsonb("localized_name"), json(location))
                .set(text("status"), location.status().name())
                .set(instant("updated_at"), location.audit().updatedAt())
                .set(text("updated_by"), location.audit().updatedBy())
                .set(version(), location.audit().version())
                .where(id().eq(location.id()).and(version().eq(expectedVersion)))
                .execute();
        requireUpdated(updated, "warehouse_location", location.id());
        return location;
    }

    @Override
    public Optional<WarehouseLocation> findLocation(final UUID id) {
        return dsl.selectFrom(table("warehouse_location")).where(id().eq(id)).fetchOptional(this::location);
    }

    @Override
    public List<WarehouseLocation> listLocationsByZone(final UUID zoneId) {
        return dsl.selectFrom(table("warehouse_location"))
                .where(uuid("zone_id").eq(zoneId))
                .orderBy(text("code"))
                .fetch(this::location);
    }

    @Override
    public boolean locationCodeExists(final UUID zoneId, final LocationCode code) {
        return exists("warehouse_location", uuid("zone_id").eq(zoneId).and(text("code").eq(code.value())));
    }

    @Override
    public long activeLocationCount(final UUID zoneId) {
        return activeCount("warehouse_location", "zone_id", zoneId);
    }

    private Enterprise enterprise(final Record record) {
        return new Enterprise(record.get(id()), new EnterpriseCode(record.get(text("code"))),
                new DisplayName(record.get(text("name"))), localized(record), status(record), audit(record));
    }

    private LegalEntity legalEntity(final Record record) {
        return new LegalEntity(record.get(id()), record.get(uuid("enterprise_id")),
                new LegalEntityCode(record.get(text("code"))), new DisplayName(record.get(text("name"))),
                localized(record), new CountryCode(record.get(text("country_code"))),
                new CurrencyCode(record.get(text("base_currency"))), status(record), audit(record));
    }

    private Company company(final Record record) {
        return new Company(record.get(id()), record.get(uuid("enterprise_id")), record.get(uuid("legal_entity_id")),
                new CompanyCode(record.get(text("code"))), new DisplayName(record.get(text("name"))),
                localized(record), new CountryCode(record.get(text("country_code"))),
                new CurrencyCode(record.get(text("base_currency"))), new TimeZoneId(record.get(text("time_zone_id"))),
                address(record), status(record), audit(record));
    }

    private Branch branch(final Record record) {
        return new Branch(record.get(id()), record.get(uuid("enterprise_id")), record.get(uuid("company_id")),
                new BranchCode(record.get(text("code"))), new DisplayName(record.get(text("name"))),
                localized(record), address(record), status(record), audit(record));
    }

    private Warehouse warehouse(final Record record) {
        return new Warehouse(record.get(id()), record.get(uuid("enterprise_id")), record.get(uuid("company_id")),
                record.get(uuid("branch_id")), new WarehouseCode(record.get(text("code"))),
                new DisplayName(record.get(text("name"))), localized(record),
                WarehouseType.valueOf(record.get(text("warehouse_type"))), record.get(text("project_reference")),
                address(record), status(record), audit(record));
    }

    private WarehouseZone zone(final Record record) {
        return new WarehouseZone(record.get(id()), record.get(uuid("enterprise_id")), record.get(uuid("company_id")),
                record.get(uuid("warehouse_id")), new ZoneCode(record.get(text("code"))),
                new DisplayName(record.get(text("name"))), localized(record), status(record), audit(record));
    }

    private WarehouseLocation location(final Record record) {
        return new WarehouseLocation(record.get(id()), record.get(uuid("enterprise_id")),
                record.get(uuid("company_id")), record.get(uuid("warehouse_id")), record.get(uuid("zone_id")),
                new LocationCode(record.get(text("code"))), new DisplayName(record.get(text("name"))),
                localized(record), status(record), audit(record));
    }

    private boolean exists(final String tableName, final org.jooq.Condition condition) {
        return dsl.fetchExists(dsl.selectOne().from(table(tableName)).where(condition));
    }

    private long activeCount(final String tableName, final String parentColumn, final UUID parentId) {
        return dsl.selectCount()
                .from(table(tableName))
                .where(uuid(parentColumn).eq(parentId).and(text("status").eq(LifecycleStatus.ACTIVE.name())))
                .fetchSingle(0, long.class);
    }

    private JSONB json(final Enterprise enterprise) {
        return json(enterprise.localizedName());
    }

    private JSONB json(final LegalEntity legalEntity) {
        return json(legalEntity.localizedName());
    }

    private JSONB json(final Company company) {
        return json(company.localizedName());
    }

    private JSONB json(final Branch branch) {
        return json(branch.localizedName());
    }

    private JSONB json(final Warehouse warehouse) {
        return json(warehouse.localizedName());
    }

    private JSONB json(final WarehouseZone zone) {
        return json(zone.localizedName());
    }

    private JSONB json(final WarehouseLocation location) {
        return json(location.localizedName());
    }

    private JSONB json(final LocalizedName localizedName) {
        try {
            return JSONB.valueOf(objectMapper.writeValueAsString(localizedName.values()));
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Invalid localized name.", exception);
        }
    }

    private LocalizedName localized(final Record record) {
        final JSONB json = record.get(jsonb("localized_name"));
        if (json == null) {
            return new LocalizedName(Map.of());
        }
        try {
            return new LocalizedName(objectMapper.readValue(json.data(), LOCALIZED_NAME_TYPE));
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Invalid localized name JSON.", exception);
        }
    }

    private LifecycleStatus status(final Record record) {
        return LifecycleStatus.valueOf(record.get(text("status")));
    }

    private AuditMetadata audit(final Record record) {
        return new AuditMetadata(record.get(instant("created_at")), record.get(text("created_by")),
                record.get(instant("updated_at")), record.get(text("updated_by")), record.get(version()));
    }

    private Address address(final Record record) {
        return new Address(record.get(text("address_line1")), record.get(text("address_line2")),
                record.get(text("city")), record.get(text("region")), record.get(text("postal_code")));
    }

    private static String line1(final Address address) {
        return address == null ? null : address.line1();
    }

    private static String line2(final Address address) {
        return address == null ? null : address.line2();
    }

    private static String city(final Address address) {
        return address == null ? null : address.city();
    }

    private static String region(final Address address) {
        return address == null ? null : address.region();
    }

    private static String postalCode(final Address address) {
        return address == null ? null : address.postalCode();
    }

    private static void requireUpdated(final int updated, final String tableName, final UUID id) {
        if (updated != 1) {
            throw new OptimisticLockConflictException("Optimistic lock conflict for " + tableName + ": " + id);
        }
    }

    private static Table<Record> table(final String name) {
        return DSL.table(DSL.name(name));
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

    private static Field<JSONB> jsonb(final String name) {
        return DSL.field(DSL.name(name), JSONB.class);
    }

    private static Field<Instant> instant(final String name) {
        return DSL.field(DSL.name(name), Instant.class);
    }

    private static Field<Long> version() {
        return DSL.field(DSL.name("version"), Long.class);
    }
}
