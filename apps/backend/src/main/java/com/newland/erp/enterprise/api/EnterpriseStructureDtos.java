package com.newland.erp.enterprise.api;

import com.newland.erp.enterprise.domain.Branch;
import com.newland.erp.enterprise.domain.Company;
import com.newland.erp.enterprise.domain.Enterprise;
import com.newland.erp.enterprise.domain.LegalEntity;
import com.newland.erp.enterprise.domain.Warehouse;
import com.newland.erp.enterprise.domain.WarehouseLocation;
import com.newland.erp.enterprise.domain.WarehouseZone;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;
import java.util.UUID;

public final class EnterpriseStructureDtos {
    private EnterpriseStructureDtos() {
    }

    public record NamePayload(
            @NotBlank @Size(max = 160) String name,
            Map<String, String> localizedName
    ) {
    }

    public record AddressPayload(
            @Size(max = 160) String line1,
            @Size(max = 160) String line2,
            @Size(max = 80) String city,
            @Size(max = 80) String region,
            @Size(max = 32) String postalCode
    ) {
    }

    public record CreateEnterpriseRequest(
            @NotBlank @Size(max = 32) String code,
            @NotBlank @Size(max = 160) String name,
            Map<String, String> localizedName
    ) {
    }

    public record UpdateEnterpriseRequest(
            @NotBlank @Size(max = 160) String name,
            Map<String, String> localizedName,
            @Min(0) long expectedVersion
    ) {
    }

    public record CreateLegalEntityRequest(
            @NotBlank @Size(max = 32) String code,
            @NotBlank @Size(max = 160) String name,
            Map<String, String> localizedName,
            @NotBlank @Size(min = 2, max = 2) String countryCode,
            @NotBlank @Size(min = 3, max = 3) String baseCurrency
    ) {
    }

    public record UpdateLegalEntityRequest(
            @NotBlank @Size(max = 160) String name,
            Map<String, String> localizedName,
            @NotBlank @Size(min = 2, max = 2) String countryCode,
            @NotBlank @Size(min = 3, max = 3) String baseCurrency,
            @Min(0) long expectedVersion
    ) {
    }

    public record CreateCompanyRequest(
            @NotBlank @Size(max = 32) String code,
            @NotBlank @Size(max = 160) String name,
            Map<String, String> localizedName,
            @NotBlank @Size(min = 2, max = 2) String countryCode,
            @NotBlank @Size(min = 3, max = 3) String baseCurrency,
            @NotBlank @Size(max = 64) String timeZoneId,
            AddressPayload address
    ) {
    }

    public record UpdateCompanyRequest(
            @NotBlank @Size(max = 160) String name,
            Map<String, String> localizedName,
            @NotBlank @Size(min = 2, max = 2) String countryCode,
            @NotBlank @Size(min = 3, max = 3) String baseCurrency,
            @NotBlank @Size(max = 64) String timeZoneId,
            AddressPayload address,
            @Min(0) long expectedVersion
    ) {
    }

    public record CreateBranchRequest(
            @NotBlank @Size(max = 32) String code,
            @NotBlank @Size(max = 160) String name,
            Map<String, String> localizedName,
            AddressPayload address
    ) {
    }

    public record UpdateBranchRequest(
            @NotBlank @Size(max = 160) String name,
            Map<String, String> localizedName,
            AddressPayload address,
            @Min(0) long expectedVersion
    ) {
    }

    public record CreateWarehouseRequest(
            UUID branchId,
            @NotBlank @Size(max = 32) String code,
            @NotBlank @Size(max = 160) String name,
            Map<String, String> localizedName,
            @NotBlank String type,
            @Size(max = 80) String projectReference,
            AddressPayload address
    ) {
    }

    public record UpdateWarehouseRequest(
            @NotBlank @Size(max = 160) String name,
            Map<String, String> localizedName,
            @NotBlank String type,
            @Size(max = 80) String projectReference,
            AddressPayload address,
            @Min(0) long expectedVersion
    ) {
    }

    public record CreateZoneRequest(
            @NotBlank @Size(max = 32) String code,
            @NotBlank @Size(max = 160) String name,
            Map<String, String> localizedName
    ) {
    }

    public record UpdateZoneRequest(
            @NotBlank @Size(max = 160) String name,
            Map<String, String> localizedName,
            @Min(0) long expectedVersion
    ) {
    }

    public record CreateLocationRequest(
            @NotBlank @Size(max = 32) String code,
            @NotBlank @Size(max = 160) String name,
            Map<String, String> localizedName
    ) {
    }

    public record UpdateLocationRequest(
            @NotBlank @Size(max = 160) String name,
            Map<String, String> localizedName,
            @Min(0) long expectedVersion
    ) {
    }

    public record EnterpriseResponse(
            @NotNull UUID id,
            String code,
            String name,
            Map<String, String> localizedName,
            String status,
            long version
    ) {
        public static EnterpriseResponse from(final Enterprise enterprise) {
            return new EnterpriseResponse(enterprise.id(), enterprise.code().value(), enterprise.name().value(),
                    enterprise.localizedName().values(), enterprise.status().name(), enterprise.audit().version());
        }
    }

    public record LegalEntityResponse(
            UUID id,
            UUID enterpriseId,
            String code,
            String name,
            Map<String, String> localizedName,
            String countryCode,
            String baseCurrency,
            String status,
            long version
    ) {
        public static LegalEntityResponse from(final LegalEntity legalEntity) {
            return new LegalEntityResponse(legalEntity.id(), legalEntity.enterpriseId(), legalEntity.code().value(),
                    legalEntity.name().value(), legalEntity.localizedName().values(),
                    legalEntity.countryCode().value(), legalEntity.baseCurrency().value(),
                    legalEntity.status().name(), legalEntity.audit().version());
        }
    }

    public record CompanyResponse(
            UUID id,
            UUID enterpriseId,
            UUID legalEntityId,
            String code,
            String name,
            Map<String, String> localizedName,
            String countryCode,
            String baseCurrency,
            String timeZoneId,
            AddressPayload address,
            String status,
            long version
    ) {
        public static CompanyResponse from(final Company company) {
            return new CompanyResponse(company.id(), company.enterpriseId(), company.legalEntityId(),
                    company.code().value(), company.name().value(), company.localizedName().values(),
                    company.countryCode().value(), company.baseCurrency().value(), company.timeZoneId().value(),
                    address(company), company.status().name(), company.audit().version());
        }
    }

    public record BranchResponse(
            UUID id,
            UUID enterpriseId,
            UUID companyId,
            String code,
            String name,
            Map<String, String> localizedName,
            AddressPayload address,
            String status,
            long version
    ) {
        public static BranchResponse from(final Branch branch) {
            return new BranchResponse(branch.id(), branch.enterpriseId(), branch.companyId(), branch.code().value(),
                    branch.name().value(), branch.localizedName().values(), address(branch), branch.status().name(),
                    branch.audit().version());
        }
    }

    public record WarehouseResponse(
            UUID id,
            UUID enterpriseId,
            UUID companyId,
            UUID branchId,
            String code,
            String name,
            Map<String, String> localizedName,
            String type,
            String projectReference,
            AddressPayload address,
            String status,
            long version
    ) {
        public static WarehouseResponse from(final Warehouse warehouse) {
            return new WarehouseResponse(warehouse.id(), warehouse.enterpriseId(), warehouse.companyId(),
                    warehouse.branchId(), warehouse.code().value(), warehouse.name().value(),
                    warehouse.localizedName().values(), warehouse.type().name(), warehouse.projectReference(),
                    address(warehouse), warehouse.status().name(), warehouse.audit().version());
        }
    }

    public record ZoneResponse(
            UUID id,
            UUID enterpriseId,
            UUID companyId,
            UUID warehouseId,
            String code,
            String name,
            Map<String, String> localizedName,
            String status,
            long version
    ) {
        public static ZoneResponse from(final WarehouseZone zone) {
            return new ZoneResponse(zone.id(), zone.enterpriseId(), zone.companyId(), zone.warehouseId(),
                    zone.code().value(), zone.name().value(), zone.localizedName().values(), zone.status().name(),
                    zone.audit().version());
        }
    }

    public record LocationResponse(
            UUID id,
            UUID enterpriseId,
            UUID companyId,
            UUID warehouseId,
            UUID zoneId,
            String code,
            String name,
            Map<String, String> localizedName,
            String status,
            long version
    ) {
        public static LocationResponse from(final WarehouseLocation location) {
            return new LocationResponse(location.id(), location.enterpriseId(), location.companyId(),
                    location.warehouseId(), location.zoneId(), location.code().value(), location.name().value(),
                    location.localizedName().values(), location.status().name(), location.audit().version());
        }
    }

    private static AddressPayload address(final Company company) {
        return address(company.address());
    }

    private static AddressPayload address(final Branch branch) {
        return address(branch.address());
    }

    private static AddressPayload address(final Warehouse warehouse) {
        return address(warehouse.address());
    }

    private static AddressPayload address(final com.newland.erp.enterprise.domain.Address address) {
        if (address == null) {
            return null;
        }
        return new AddressPayload(address.line1(), address.line2(), address.city(), address.region(),
                address.postalCode());
    }
}
