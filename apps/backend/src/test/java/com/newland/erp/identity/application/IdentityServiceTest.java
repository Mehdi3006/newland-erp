package com.newland.erp.identity.application;

import com.newland.erp.identity.domain.EmailAddress;
import com.newland.erp.identity.domain.OrganizationScope;
import com.newland.erp.identity.domain.PasswordCredential;
import com.newland.erp.identity.domain.Permission;
import com.newland.erp.identity.domain.RefreshToken;
import com.newland.erp.identity.domain.Role;
import com.newland.erp.identity.domain.RolePermissionAssignment;
import com.newland.erp.identity.domain.ScopeType;
import com.newland.erp.identity.domain.Session;
import com.newland.erp.identity.domain.User;
import com.newland.erp.identity.domain.UserRoleAssignment;
import com.newland.erp.identity.domain.UserStatus;
import com.newland.erp.identity.domain.Username;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

final class IdentityServiceTest {
    @Test
    void resolvesCapabilitiesOnlyInsideAssignedOrganizationScope() {
        final FakeIdentityRepository repository = new FakeIdentityRepository();
        final IdentityService service = new IdentityService(repository, new PlainTestHasher(), new FakeTokenService(),
                event -> {
                }, (ApplicationEventPublisher) event -> {
                }, Clock.fixed(Instant.parse("2026-07-20T00:00:00Z"), ZoneOffset.UTC));
        final UUID companyScopeId = UUID.randomUUID();
        final UUID otherCompanyScopeId = UUID.randomUUID();
        final User user = service.createUser(new IdentityCommands.CreateUser(new Username("owner"),
                new EmailAddress("owner@example.com"), "Owner", "StrongPass123"), "tester");
        final Role role = service.createRole(new IdentityCommands.CreateRole("identity-manager",
                "Identity Manager", null), "tester");
        final Permission permission = service.createPermission(new IdentityCommands.CreatePermission(
                IdentityPermissions.USER_MANAGE, "Manage users"), "tester");

        service.assignPermission(new IdentityCommands.AssignPermission(role.id(), permission.id()), "tester");
        service.assignRole(new IdentityCommands.AssignRole(user.id(), role.id(),
                new OrganizationScope(ScopeType.COMPANY, companyScopeId)), "tester");

        assertThat(service.decide(user.id(), IdentityPermissions.USER_MANAGE,
                new OrganizationScope(ScopeType.COMPANY, companyScopeId)).granted()).isTrue();
        assertThat(service.decide(user.id(), IdentityPermissions.USER_MANAGE,
                new OrganizationScope(ScopeType.COMPANY, otherCompanyScopeId)).granted()).isFalse();
    }

    @Test
    void grantsRepositoryGlobalCapabilityOnlyThroughSystemRoleAtEnterpriseScope() {
        final FakeIdentityRepository repository = new FakeIdentityRepository();
        final IdentityService service = service(repository);
        final UUID enterpriseId = UUID.randomUUID();
        final User user = service.createUser(new IdentityCommands.CreateUser(new Username("global-owner"),
                new EmailAddress("global-owner@example.com"), "Global Owner", "StrongPass123"), "tester");
        final Permission permission = service.createPermission(new IdentityCommands.CreatePermission(
                "finance.posting.rule.manage", "Manage global posting rules"), "tester");
        final Role ordinaryRole = service.createRole(new IdentityCommands.CreateRole("finance-manager",
                "Finance Manager", null), "tester");
        service.assignPermission(new IdentityCommands.AssignPermission(ordinaryRole.id(), permission.id()), "tester");
        service.assignRole(new IdentityCommands.AssignRole(user.id(), ordinaryRole.id(),
                new OrganizationScope(ScopeType.ENTERPRISE, enterpriseId)), "tester");

        assertThat(service.isSystemEnterpriseCapabilityGranted(
                user.id(), "finance.posting.rule.manage")).isFalse();

        final Role systemRole = repository.insertRole(new Role(UUID.randomUUID(), "SYSTEM_FINANCE",
                "System Finance", null, true));
        service.assignPermission(new IdentityCommands.AssignPermission(systemRole.id(), permission.id()), "tester");
        service.assignRole(new IdentityCommands.AssignRole(user.id(), systemRole.id(),
                new OrganizationScope(ScopeType.ENTERPRISE, enterpriseId)), "tester");

        assertThat(service.isSystemEnterpriseCapabilityGranted(
                user.id(), "finance.posting.rule.manage")).isTrue();

        repository.updateUser(new User(user.id(), user.username(), user.email(), user.displayName(),
                UserStatus.DISABLED, user.failedLoginAttempts(), user.lockedUntil(), user.passwordExpiresAt(),
                user.createdAt(), user.updatedAt()));

        assertThat(service.isSystemEnterpriseCapabilityGranted(
                user.id(), "finance.posting.rule.manage")).isFalse();
    }

    @Test
    void disabledUserLoginDoesNotMutateAccountState() {
        final FakeIdentityRepository repository = new FakeIdentityRepository();
        final IdentityService service = service(repository);
        final User user = service.createUser(new IdentityCommands.CreateUser(new Username("disabled"),
                new EmailAddress("disabled@example.com"), "Disabled", "StrongPass123"), "tester");
        repository.updateUser(new User(user.id(), user.username(), user.email(), user.displayName(),
                UserStatus.DISABLED, user.failedLoginAttempts(), user.lockedUntil(), user.passwordExpiresAt(),
                user.createdAt(), user.updatedAt()));

        assertThatThrownBy(() -> service.login(new IdentityCommands.Login(user.username(), "WrongPass123",
                "browser", false))).isInstanceOf(com.newland.erp.identity.domain.AuthenticationFailedException.class);

        final User stored = repository.findUser(user.id()).orElseThrow();
        assertThat(stored.status()).isEqualTo(UserStatus.DISABLED);
        assertThat(stored.failedLoginAttempts()).isZero();
    }

    @Test
    void refreshRejectsRevokedSessionEvenWhenRefreshTokenIsOtherwiseUsable() {
        final FakeIdentityRepository repository = new FakeIdentityRepository();
        final IdentityService service = service(repository);
        final User user = service.createUser(new IdentityCommands.CreateUser(new Username("owner"),
                new EmailAddress("owner@example.com"), "Owner", "StrongPass123"), "tester");
        final AuthTokens tokens = service.login(new IdentityCommands.Login(user.username(), "StrongPass123",
                "browser", false));
        service.revokeSession(tokens.sessionId(), "tester");

        assertThatThrownBy(() -> service.refresh(tokens.refreshToken()))
                .isInstanceOf(com.newland.erp.identity.domain.AuthenticationFailedException.class);
    }

    private static IdentityService service(final FakeIdentityRepository repository) {
        return new IdentityService(repository, new PlainTestHasher(), new FakeTokenService(),
                event -> {
                }, (ApplicationEventPublisher) event -> {
                }, Clock.fixed(Instant.parse("2026-07-20T00:00:00Z"), ZoneOffset.UTC));
    }

    private static final class PlainTestHasher implements PasswordHasher {
        @Override
        public String hash(final String rawPassword) {
            return "$argon2id$" + rawPassword;
        }

        @Override
        public boolean matches(final String rawPassword, final String hash) {
            return hash.equals("$argon2id$" + rawPassword);
        }
    }

    private static final class FakeTokenService implements TokenService {
        @Override
        public String issueAccessToken(final User user, final Set<String> capabilities, final UUID sessionId,
                                       final Instant expiresAt) {
            return "access";
        }

        @Override
        public String newRefreshToken() {
            return "refresh";
        }

        @Override
        public String tokenHash(final String token) {
            return token;
        }
    }

    private static final class FakeIdentityRepository implements IdentityRepository {
        private final Map<UUID, User> users = new HashMap<>();
        private final Map<UUID, Role> roles = new HashMap<>();
        private final Map<UUID, Permission> permissions = new HashMap<>();
        private final Map<UUID, PasswordCredential> credentials = new HashMap<>();
        private final Map<UUID, Session> sessions = new HashMap<>();
        private final Map<String, RefreshToken> refreshTokensByHash = new HashMap<>();
        private final List<UserRoleAssignment> userRoles = new ArrayList<>();
        private final List<RolePermissionAssignment> rolePermissions = new ArrayList<>();

        @Override
        public boolean usernameExists(final Username username) {
            return users.values().stream().anyMatch(user -> user.username().equals(username));
        }

        @Override
        public boolean roleCodeExists(final String roleCode) {
            return roles.values().stream().anyMatch(role -> role.code().equals(roleCode.toUpperCase()));
        }

        @Override
        public boolean capabilityExists(final String capability) {
            return permissions.values().stream().anyMatch(permission -> permission.capability().value()
                    .equals(capability));
        }

        @Override
        public User insertUser(final User user) {
            users.put(user.id(), user);
            return user;
        }

        @Override
        public User updateUser(final User user) {
            users.put(user.id(), user);
            return user;
        }

        @Override
        public Optional<User> findUser(final UUID id) {
            return Optional.ofNullable(users.get(id));
        }

        @Override
        public Optional<User> findUserByUsername(final Username username) {
            return users.values().stream().filter(user -> user.username().equals(username)).findFirst();
        }

        @Override
        public List<User> listUsers() {
            return List.copyOf(users.values());
        }

        @Override
        public PasswordCredential insertCredential(final PasswordCredential credential) {
            credentials.put(credential.userId(), credential);
            return credential;
        }

        @Override
        public Optional<PasswordCredential> findCurrentCredential(final UUID userId) {
            return Optional.ofNullable(credentials.get(userId));
        }

        @Override
        public void archiveCurrentCredential(final UUID userId) {
            credentials.remove(userId);
        }

        @Override
        public Role insertRole(final Role role) {
            roles.put(role.id(), role);
            return role;
        }

        @Override
        public Role updateRole(final Role role) {
            roles.put(role.id(), role);
            return role;
        }

        @Override
        public Optional<Role> findRole(final UUID id) {
            return Optional.ofNullable(roles.get(id));
        }

        @Override
        public List<Role> listRoles() {
            return List.copyOf(roles.values());
        }

        @Override
        public Permission insertPermission(final Permission permission) {
            permissions.put(permission.id(), permission);
            return permission;
        }

        @Override
        public Optional<Permission> findPermission(final UUID id) {
            return Optional.ofNullable(permissions.get(id));
        }

        @Override
        public Optional<Permission> findPermissionByCapability(final String capability) {
            return permissions.values().stream().filter(permission -> permission.capability().value()
                    .equals(capability)).findFirst();
        }

        @Override
        public List<Permission> listPermissions() {
            return List.copyOf(permissions.values());
        }

        @Override
        public UserRoleAssignment insertUserRoleAssignment(final UserRoleAssignment assignment) {
            userRoles.add(assignment);
            return assignment;
        }

        @Override
        public void removeUserRoleAssignment(final UUID assignmentId) {
            userRoles.removeIf(assignment -> assignment.id().equals(assignmentId));
        }

        @Override
        public List<UserRoleAssignment> listUserRoleAssignments(final UUID userId) {
            return userRoles.stream().filter(assignment -> assignment.userId().equals(userId)).toList();
        }

        @Override
        public RolePermissionAssignment insertRolePermissionAssignment(final RolePermissionAssignment assignment) {
            rolePermissions.add(assignment);
            return assignment;
        }

        @Override
        public List<RolePermissionAssignment> listRolePermissionAssignments(final UUID roleId) {
            return rolePermissions.stream().filter(assignment -> assignment.roleId().equals(roleId)).toList();
        }

        @Override
        public Session insertSession(final Session session) {
            sessions.put(session.id(), session);
            return session;
        }

        @Override
        public Session updateSession(final Session session) {
            sessions.put(session.id(), session);
            return session;
        }

        @Override
        public Optional<Session> findSession(final UUID id) {
            return Optional.ofNullable(sessions.get(id));
        }

        @Override
        public List<Session> listSessions(final UUID userId) {
            return sessions.values().stream().filter(session -> session.userId().equals(userId)).toList();
        }

        @Override
        public RefreshToken insertRefreshToken(final RefreshToken token) {
            refreshTokensByHash.put(token.tokenHash(), token);
            return token;
        }

        @Override
        public RefreshToken updateRefreshToken(final RefreshToken token) {
            refreshTokensByHash.put(token.tokenHash(), token);
            return token;
        }

        @Override
        public Optional<RefreshToken> findRefreshTokenByHash(final String tokenHash) {
            return Optional.ofNullable(refreshTokensByHash.get(tokenHash));
        }
    }
}
