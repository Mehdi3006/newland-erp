package com.newland.erp.identity.domain;

import java.time.Instant;
import java.util.UUID;

public record UserRoleAssignment(UUID id, UUID userId, UUID roleId, OrganizationScope scope, Instant assignedAt) {
    public UserRoleAssignment {
        Permission.require(id, "assignment id");
        Permission.require(userId, "user id");
        Permission.require(roleId, "role id");
        Permission.require(scope, "organization scope");
        Permission.require(assignedAt, "assigned at");
    }
}
