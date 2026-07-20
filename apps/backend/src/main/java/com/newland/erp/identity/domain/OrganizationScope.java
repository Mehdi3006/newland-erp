package com.newland.erp.identity.domain;

import java.util.UUID;

public record OrganizationScope(ScopeType type, UUID scopeId) {
    public OrganizationScope {
        if (type == null || scopeId == null) {
            throw new IllegalArgumentException("Organization scope type and id are required.");
        }
    }
}
