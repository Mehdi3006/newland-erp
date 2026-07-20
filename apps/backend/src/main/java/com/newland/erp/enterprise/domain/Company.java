package com.newland.erp.enterprise.domain;

import java.util.UUID;

public record Company(
        UUID id,
        UUID enterpriseId,
        UUID legalEntityId,
        CompanyCode code,
        DisplayName name,
        LocalizedName localizedName,
        CountryCode countryCode,
        CurrencyCode baseCurrency,
        TimeZoneId timeZoneId,
        Address address,
        LifecycleStatus status,
        AuditMetadata audit
) {
    public Company {
        Enterprise.requireId(id);
        Enterprise.requireId(enterpriseId);
        Enterprise.requireId(legalEntityId);
        Enterprise.require(code, "company code");
        Enterprise.require(name, "company name");
        Enterprise.require(localizedName, "company localized name");
        Enterprise.require(countryCode, "company country");
        Enterprise.require(baseCurrency, "company base currency");
        Enterprise.require(timeZoneId, "company time zone");
        Enterprise.require(status, "company status");
        Enterprise.require(audit, "company audit");
    }

    public Company update(
            final DisplayName newName,
            final LocalizedName newLocalizedName,
            final CountryCode newCountryCode,
            final CurrencyCode newBaseCurrency,
            final TimeZoneId newTimeZoneId,
            final Address newAddress,
            final AuditMetadata nextAudit
    ) {
        return new Company(id, enterpriseId, legalEntityId, code, newName, newLocalizedName, newCountryCode,
                newBaseCurrency, newTimeZoneId, newAddress, status, nextAudit);
    }

    public Company activate(final AuditMetadata nextAudit) {
        return new Company(id, enterpriseId, legalEntityId, code, name, localizedName, countryCode, baseCurrency,
                timeZoneId, address, status.activate(), nextAudit);
    }

    public Company deactivate(final AuditMetadata nextAudit) {
        return new Company(id, enterpriseId, legalEntityId, code, name, localizedName, countryCode, baseCurrency,
                timeZoneId, address, status.deactivate(), nextAudit);
    }
}
