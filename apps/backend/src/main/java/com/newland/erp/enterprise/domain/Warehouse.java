package com.newland.erp.enterprise.domain;

import java.util.UUID;

public record Warehouse(
        UUID id,
        UUID enterpriseId,
        UUID companyId,
        UUID branchId,
        WarehouseCode code,
        DisplayName name,
        LocalizedName localizedName,
        WarehouseType type,
        String projectReference,
        Address address,
        LifecycleStatus status,
        AuditMetadata audit
) {
    public Warehouse {
        Enterprise.requireId(id);
        Enterprise.requireId(enterpriseId);
        Enterprise.requireId(companyId);
        Enterprise.require(code, "warehouse code");
        Enterprise.require(name, "warehouse name");
        Enterprise.require(localizedName, "warehouse localized name");
        Enterprise.require(type, "warehouse type");
        Enterprise.require(status, "warehouse status");
        Enterprise.require(audit, "warehouse audit");
        if (type == WarehouseType.BRANCH && branchId == null) {
            throw new IllegalArgumentException("Branch warehouse requires a branch.");
        }
        if (type == WarehouseType.PROJECT && (projectReference == null || projectReference.isBlank())) {
            throw new IllegalArgumentException("Project warehouse requires a project reference.");
        }
    }

    public Warehouse update(
            final DisplayName newName,
            final LocalizedName newLocalizedName,
            final WarehouseType newType,
            final String newProjectReference,
            final Address newAddress,
            final AuditMetadata nextAudit
    ) {
        return new Warehouse(id, enterpriseId, companyId, branchId, code, newName, newLocalizedName, newType,
                newProjectReference, newAddress, status, nextAudit);
    }

    public Warehouse activate(final AuditMetadata nextAudit) {
        return new Warehouse(id, enterpriseId, companyId, branchId, code, name, localizedName, type, projectReference,
                address, status.activate(), nextAudit);
    }

    public Warehouse deactivate(final AuditMetadata nextAudit) {
        return new Warehouse(id, enterpriseId, companyId, branchId, code, name, localizedName, type, projectReference,
                address, status.deactivate(), nextAudit);
    }
}
