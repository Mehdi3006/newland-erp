package com.newland.erp.enterprise.application;

import com.newland.erp.enterprise.domain.Branch;
import com.newland.erp.enterprise.domain.BranchCode;
import com.newland.erp.enterprise.domain.Company;
import com.newland.erp.enterprise.domain.CompanyCode;
import com.newland.erp.enterprise.domain.Enterprise;
import com.newland.erp.enterprise.domain.EnterpriseCode;
import com.newland.erp.enterprise.domain.LegalEntity;
import com.newland.erp.enterprise.domain.LegalEntityCode;
import com.newland.erp.enterprise.domain.LocationCode;
import com.newland.erp.enterprise.domain.Warehouse;
import com.newland.erp.enterprise.domain.WarehouseCode;
import com.newland.erp.enterprise.domain.WarehouseLocation;
import com.newland.erp.enterprise.domain.WarehouseZone;
import com.newland.erp.enterprise.domain.ZoneCode;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EnterpriseStructureRepository {
    Enterprise insertEnterprise(Enterprise enterprise);

    Enterprise updateEnterprise(Enterprise enterprise, long expectedVersion);

    Optional<Enterprise> findEnterprise(UUID id);

    List<Enterprise> listEnterprises();

    boolean enterpriseCodeExists(EnterpriseCode code);

    LegalEntity insertLegalEntity(LegalEntity legalEntity);

    LegalEntity updateLegalEntity(LegalEntity legalEntity, long expectedVersion);

    Optional<LegalEntity> findLegalEntity(UUID id);

    List<LegalEntity> listLegalEntitiesByEnterprise(UUID enterpriseId);

    boolean legalEntityCodeExists(UUID enterpriseId, LegalEntityCode code);

    long activeLegalEntityCount(UUID enterpriseId);

    Company insertCompany(Company company);

    Company updateCompany(Company company, long expectedVersion);

    Optional<Company> findCompany(UUID id);

    List<Company> listCompaniesByLegalEntity(UUID legalEntityId);

    boolean companyCodeExists(UUID enterpriseId, CompanyCode code);

    long activeCompanyCount(UUID legalEntityId);

    Branch insertBranch(Branch branch);

    Branch updateBranch(Branch branch, long expectedVersion);

    Optional<Branch> findBranch(UUID id);

    List<Branch> listBranchesByCompany(UUID companyId);

    boolean branchCodeExists(UUID companyId, BranchCode code);

    long activeBranchCount(UUID companyId);

    Warehouse insertWarehouse(Warehouse warehouse);

    Warehouse updateWarehouse(Warehouse warehouse, long expectedVersion);

    Optional<Warehouse> findWarehouse(UUID id);

    List<Warehouse> listWarehousesByCompany(UUID companyId);

    List<Warehouse> listWarehousesByBranch(UUID branchId);

    boolean warehouseCodeExists(UUID companyId, WarehouseCode code);

    long activeWarehouseCountByCompany(UUID companyId);

    long activeWarehouseCountByBranch(UUID branchId);

    WarehouseZone insertZone(WarehouseZone zone);

    WarehouseZone updateZone(WarehouseZone zone, long expectedVersion);

    Optional<WarehouseZone> findZone(UUID id);

    List<WarehouseZone> listZonesByWarehouse(UUID warehouseId);

    boolean zoneCodeExists(UUID warehouseId, ZoneCode code);

    long activeZoneCount(UUID warehouseId);

    WarehouseLocation insertLocation(WarehouseLocation location);

    WarehouseLocation updateLocation(WarehouseLocation location, long expectedVersion);

    Optional<WarehouseLocation> findLocation(UUID id);

    List<WarehouseLocation> listLocationsByZone(UUID zoneId);

    boolean locationCodeExists(UUID zoneId, LocationCode code);

    long activeLocationCount(UUID zoneId);
}
