package com.newland.erp.identity.application;

import com.newland.erp.identity.domain.EmailAddress;
import com.newland.erp.identity.domain.OrganizationScope;
import com.newland.erp.identity.domain.Username;

import java.util.UUID;

public final class IdentityCommands {
    public record CreateUser(Username username, EmailAddress email, String displayName, String password) {
    }

    public record UpdateUser(UUID id, EmailAddress email, String displayName) {
    }

    public record CreateRole(String code, String name, String description) {
    }

    public record CreatePermission(String capability, String description) {
    }

    public record AssignRole(UUID userId, UUID roleId, OrganizationScope scope) {
    }

    public record AssignPermission(UUID roleId, UUID permissionId) {
    }

    public record Login(Username username, String password, String deviceLabel, boolean rememberMe) {
    }

    public record ChangePassword(UUID userId, String currentPassword, String newPassword) {
    }

    private IdentityCommands() {
    }
}
