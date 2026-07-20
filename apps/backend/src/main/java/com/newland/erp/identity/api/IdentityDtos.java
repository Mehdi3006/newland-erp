package com.newland.erp.identity.api;

import com.newland.erp.identity.application.AuthTokens;
import com.newland.erp.identity.application.AuthorizationDecision;
import com.newland.erp.identity.domain.OrganizationScope;
import com.newland.erp.identity.domain.Permission;
import com.newland.erp.identity.domain.Role;
import com.newland.erp.identity.domain.ScopeType;
import com.newland.erp.identity.domain.Session;
import com.newland.erp.identity.domain.User;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class IdentityDtos {
    public record LoginRequest(@NotBlank String username, @NotBlank String password, String deviceLabel,
                               boolean rememberMe) {
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    public record ChangePasswordRequest(@NotBlank String currentPassword, @NotBlank String newPassword) {
    }

    public record CreateUserRequest(@NotBlank String username, @Email String email,
                                    @NotBlank @Size(max = 160) String displayName, @NotBlank String password) {
    }

    public record UpdateUserRequest(@Email String email, @NotBlank @Size(max = 160) String displayName) {
    }

    public record CreateRoleRequest(@NotBlank String code, @NotBlank String name, String description) {
    }

    public record CreatePermissionRequest(@NotBlank String capability, String description) {
    }

    public record ScopePayload(@NotNull ScopeType type, @NotNull UUID scopeId) {
        OrganizationScope toDomain() {
            return new OrganizationScope(type, scopeId);
        }
    }

    public record AssignRoleRequest(@NotNull UUID userId, @NotNull UUID roleId, @NotNull ScopePayload scope) {
    }

    public record AssignPermissionRequest(@NotNull UUID roleId, @NotNull UUID permissionId) {
    }

    public record AuthorizationRequest(@NotNull UUID userId, @NotBlank String capability,
                                       @NotNull ScopePayload scope) {
    }

    public record TokenResponse(String accessToken, String refreshToken, Instant accessTokenExpiresAt,
                                Instant refreshTokenExpiresAt, UUID sessionId) {
        static TokenResponse from(final AuthTokens tokens) {
            return new TokenResponse(tokens.accessToken(), tokens.refreshToken(), tokens.accessTokenExpiresAt(),
                    tokens.refreshTokenExpiresAt(), tokens.sessionId());
        }
    }

    public record UserResponse(UUID id, String username, String email, String displayName, String status,
                               Instant passwordExpiresAt) {
        static UserResponse from(final User user) {
            return new UserResponse(user.id(), user.username().value(), user.email().value(), user.displayName(),
                    user.status().name(), user.passwordExpiresAt());
        }
    }

    public record RoleResponse(UUID id, String code, String name, String description, boolean systemRole) {
        static RoleResponse from(final Role role) {
            return new RoleResponse(role.id(), role.code(), role.name(), role.description(), role.systemRole());
        }
    }

    public record PermissionResponse(UUID id, String capability, String description) {
        static PermissionResponse from(final Permission permission) {
            return new PermissionResponse(permission.id(), permission.capability().value(), permission.description());
        }
    }

    public record SessionResponse(UUID id, UUID userId, String deviceLabel, Instant createdAt, Instant expiresAt,
                                  Instant revokedAt) {
        static SessionResponse from(final Session session) {
            return new SessionResponse(session.id(), session.userId(), session.deviceLabel(), session.createdAt(),
                    session.expiresAt(), session.revokedAt());
        }
    }

    public record AuthorizationResponse(boolean granted, String capability, ScopePayload scope, String reason) {
        static AuthorizationResponse from(final AuthorizationDecision decision) {
            return new AuthorizationResponse(decision.granted(), decision.capability(),
                    new ScopePayload(decision.scope().type(), decision.scope().scopeId()), decision.reason());
        }
    }

    private IdentityDtos() {
    }
}
