package com.newland.erp.enterprise.domain;

import java.util.UUID;

public record Branch(
        UUID id,
        UUID enterpriseId,
        UUID companyId,
        BranchCode code,
        DisplayName name,
        LocalizedName localizedName,
        Address address,
        LifecycleStatus status,
        AuditMetadata audit
) {
    public Branch {
        Enterprise.requireId(id);
        Enterprise.requireId(enterpriseId);
        Enterprise.requireId(companyId);
        Enterprise.require(code, "branch code");
        Enterprise.require(name, "branch name");
        Enterprise.require(localizedName, "branch localized name");
        Enterprise.require(status, "branch status");
        Enterprise.require(audit, "branch audit");
    }

    public Branch update(final DisplayName newName, final LocalizedName newLocalizedName, final Address newAddress,
            final AuditMetadata nextAudit) {
        return new Branch(id, enterpriseId, companyId, code, newName, newLocalizedName, newAddress, status, nextAudit);
    }

    public Branch activate(final AuditMetadata nextAudit) {
        return new Branch(id, enterpriseId, companyId, code, name, localizedName, address, status.activate(), nextAudit);
    }

    public Branch deactivate(final AuditMetadata nextAudit) {
        return new Branch(id, enterpriseId, companyId, code, name, localizedName, address, status.deactivate(), nextAudit);
    }
}
