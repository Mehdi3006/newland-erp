package com.newland.erp.enterprise.domain;

import java.util.UUID;

public record WarehouseZone(
        UUID id,
        UUID enterpriseId,
        UUID companyId,
        UUID warehouseId,
        ZoneCode code,
        DisplayName name,
        LocalizedName localizedName,
        LifecycleStatus status,
        AuditMetadata audit
) {
    public WarehouseZone {
        Enterprise.requireId(id);
        Enterprise.requireId(enterpriseId);
        Enterprise.requireId(companyId);
        Enterprise.requireId(warehouseId);
        Enterprise.require(code, "zone code");
        Enterprise.require(name, "zone name");
        Enterprise.require(localizedName, "zone localized name");
        Enterprise.require(status, "zone status");
        Enterprise.require(audit, "zone audit");
    }

    public WarehouseZone update(final DisplayName newName, final LocalizedName newLocalizedName,
            final AuditMetadata nextAudit) {
        return new WarehouseZone(id, enterpriseId, companyId, warehouseId, code, newName, newLocalizedName,
                status, nextAudit);
    }

    public WarehouseZone activate(final AuditMetadata nextAudit) {
        return new WarehouseZone(id, enterpriseId, companyId, warehouseId, code, name, localizedName,
                status.activate(), nextAudit);
    }

    public WarehouseZone deactivate(final AuditMetadata nextAudit) {
        return new WarehouseZone(id, enterpriseId, companyId, warehouseId, code, name, localizedName,
                status.deactivate(), nextAudit);
    }
}
