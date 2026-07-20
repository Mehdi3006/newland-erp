package com.newland.erp.enterprise.domain;

import java.util.UUID;

public record LegalEntity(
        UUID id,
        UUID enterpriseId,
        LegalEntityCode code,
        DisplayName name,
        LocalizedName localizedName,
        CountryCode countryCode,
        CurrencyCode baseCurrency,
        LifecycleStatus status,
        AuditMetadata audit
) {
    public LegalEntity {
        Enterprise.requireId(id);
        Enterprise.requireId(enterpriseId);
        Enterprise.require(code, "legal entity code");
        Enterprise.require(name, "legal entity name");
        Enterprise.require(localizedName, "legal entity localized name");
        Enterprise.require(countryCode, "legal entity country");
        Enterprise.require(baseCurrency, "legal entity base currency");
        Enterprise.require(status, "legal entity status");
        Enterprise.require(audit, "legal entity audit");
    }

    public LegalEntity update(
            final DisplayName newName,
            final LocalizedName newLocalizedName,
            final CountryCode newCountryCode,
            final CurrencyCode newBaseCurrency,
            final AuditMetadata nextAudit
    ) {
        return new LegalEntity(id, enterpriseId, code, newName, newLocalizedName, newCountryCode, newBaseCurrency,
                status, nextAudit);
    }

    public LegalEntity activate(final AuditMetadata nextAudit) {
        return new LegalEntity(id, enterpriseId, code, name, localizedName, countryCode, baseCurrency,
                status.activate(), nextAudit);
    }

    public LegalEntity deactivate(final AuditMetadata nextAudit) {
        return new LegalEntity(id, enterpriseId, code, name, localizedName, countryCode, baseCurrency,
                status.deactivate(), nextAudit);
    }
}
