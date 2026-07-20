package com.newland.erp.enterprise.domain;

import java.util.UUID;

public record WarehouseLocation(
        UUID id,
        UUID enterpriseId,
        UUID companyId,
        UUID warehouseId,
        UUID zoneId,
        LocationCode code,
        DisplayName name,
        LocalizedName localizedName,
        LifecycleStatus status,
        AuditMetadata audit
) {
    public WarehouseLocation {
        Enterprise.requireId(id);
        Enterprise.requireId(enterpriseId);
        Enterprise.requireId(companyId);
        Enterprise.requireId(warehouseId);
        Enterprise.requireId(zoneId);
        Enterprise.require(code, "location code");
        Enterprise.require(name, "location name");
        Enterprise.require(localizedName, "location localized name");
        Enterprise.require(status, "location status");
        Enterprise.require(audit, "location audit");
    }

    public WarehouseLocation update(final DisplayName newName, final LocalizedName newLocalizedName,
            final AuditMetadata nextAudit) {
        return new WarehouseLocation(id, enterpriseId, companyId, warehouseId, zoneId, code, newName, newLocalizedName,
                status, nextAudit);
    }

    public WarehouseLocation activate(final AuditMetadata nextAudit) {
        return new WarehouseLocation(id, enterpriseId, companyId, warehouseId, zoneId, code, name, localizedName,
                status.activate(), nextAudit);
    }

    public WarehouseLocation deactivate(final AuditMetadata nextAudit) {
        return new WarehouseLocation(id, enterpriseId, companyId, warehouseId, zoneId, code, name, localizedName,
                status.deactivate(), nextAudit);
    }
}
