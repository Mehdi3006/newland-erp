package com.newland.erp.enterprise.domain;

import java.util.UUID;

public record Enterprise(
        UUID id,
        EnterpriseCode code,
        DisplayName name,
        LocalizedName localizedName,
        LifecycleStatus status,
        AuditMetadata audit
) {
    public Enterprise {
        requireId(id);
        require(code, "enterprise code");
        require(name, "enterprise name");
        require(localizedName, "enterprise localized name");
        require(status, "enterprise status");
        require(audit, "enterprise audit");
    }

    public Enterprise rename(final DisplayName newName, final LocalizedName newLocalizedName, final AuditMetadata nextAudit) {
        return new Enterprise(id, code, newName, newLocalizedName, status, nextAudit);
    }

    public Enterprise activate(final AuditMetadata nextAudit) {
        return new Enterprise(id, code, name, localizedName, status.activate(), nextAudit);
    }

    public Enterprise deactivate(final AuditMetadata nextAudit) {
        return new Enterprise(id, code, name, localizedName, status.deactivate(), nextAudit);
    }

    static void requireId(final UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("id is required.");
        }
    }

    static void require(final Object value, final String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " is required.");
        }
    }
}
