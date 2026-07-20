package com.newland.erp.enterprise.application;

import com.newland.erp.enterprise.domain.Address;
import com.newland.erp.enterprise.domain.BranchCode;
import com.newland.erp.enterprise.domain.CompanyCode;
import com.newland.erp.enterprise.domain.CountryCode;
import com.newland.erp.enterprise.domain.CurrencyCode;
import com.newland.erp.enterprise.domain.DisplayName;
import com.newland.erp.enterprise.domain.EnterpriseCode;
import com.newland.erp.enterprise.domain.LegalEntityCode;
import com.newland.erp.enterprise.domain.LocalizedName;
import com.newland.erp.enterprise.domain.LocationCode;
import com.newland.erp.enterprise.domain.TimeZoneId;
import com.newland.erp.enterprise.domain.WarehouseCode;
import com.newland.erp.enterprise.domain.WarehouseType;
import com.newland.erp.enterprise.domain.ZoneCode;

import java.util.UUID;

public final class EnterpriseStructureCommands {
    private EnterpriseStructureCommands() {
    }

    public record CreateEnterprise(EnterpriseCode code, DisplayName name, LocalizedName localizedName) {
    }

    public record UpdateEnterprise(UUID id, DisplayName name, LocalizedName localizedName, long expectedVersion) {
    }

    public record CreateLegalEntity(
            UUID enterpriseId,
            LegalEntityCode code,
            DisplayName name,
            LocalizedName localizedName,
            CountryCode countryCode,
            CurrencyCode baseCurrency
    ) {
    }

    public record UpdateLegalEntity(
            UUID id,
            DisplayName name,
            LocalizedName localizedName,
            CountryCode countryCode,
            CurrencyCode baseCurrency,
            long expectedVersion
    ) {
    }

    public record CreateCompany(
            UUID legalEntityId,
            CompanyCode code,
            DisplayName name,
            LocalizedName localizedName,
            CountryCode countryCode,
            CurrencyCode baseCurrency,
            TimeZoneId timeZoneId,
            Address address
    ) {
    }

    public record UpdateCompany(
            UUID id,
            DisplayName name,
            LocalizedName localizedName,
            CountryCode countryCode,
            CurrencyCode baseCurrency,
            TimeZoneId timeZoneId,
            Address address,
            long expectedVersion
    ) {
    }

    public record CreateBranch(
            UUID companyId,
            BranchCode code,
            DisplayName name,
            LocalizedName localizedName,
            Address address
    ) {
    }

    public record UpdateBranch(UUID id, DisplayName name, LocalizedName localizedName, Address address,
            long expectedVersion) {
    }

    public record CreateWarehouse(
            UUID companyId,
            UUID branchId,
            WarehouseCode code,
            DisplayName name,
            LocalizedName localizedName,
            WarehouseType type,
            String projectReference,
            Address address
    ) {
    }

    public record UpdateWarehouse(
            UUID id,
            DisplayName name,
            LocalizedName localizedName,
            WarehouseType type,
            String projectReference,
            Address address,
            long expectedVersion
    ) {
    }

    public record CreateZone(UUID warehouseId, ZoneCode code, DisplayName name, LocalizedName localizedName) {
    }

    public record UpdateZone(UUID id, DisplayName name, LocalizedName localizedName, long expectedVersion) {
    }

    public record CreateLocation(UUID zoneId, LocationCode code, DisplayName name, LocalizedName localizedName) {
    }

    public record UpdateLocation(UUID id, DisplayName name, LocalizedName localizedName, long expectedVersion) {
    }
}
