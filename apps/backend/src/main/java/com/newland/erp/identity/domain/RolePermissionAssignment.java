package com.newland.erp.identity.domain;

import java.time.Instant;
import java.util.UUID;

public record RolePermissionAssignment(UUID id, UUID roleId, UUID permissionId, Instant assignedAt) {
    public RolePermissionAssignment {
        Permission.require(id, "role permission assignment id");
        Permission.require(roleId, "role id");
        Permission.require(permissionId, "permission id");
        Permission.require(assignedAt, "assigned at");
    }
}
