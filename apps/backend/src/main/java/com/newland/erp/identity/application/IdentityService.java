package com.newland.erp.identity.application;

import com.newland.erp.identity.domain.AuthenticationFailedException;
import com.newland.erp.identity.domain.Capability;
import com.newland.erp.identity.domain.IdentityConflictException;
import com.newland.erp.identity.domain.IdentityNotFoundException;
import com.newland.erp.identity.domain.OrganizationScope;
import com.newland.erp.identity.domain.ScopeType;
import com.newland.erp.identity.domain.PasswordCredential;
import com.newland.erp.identity.domain.Permission;
import com.newland.erp.identity.domain.RefreshToken;
import com.newland.erp.identity.domain.Role;
import com.newland.erp.identity.domain.RolePermissionAssignment;
import com.newland.erp.identity.domain.Session;
import com.newland.erp.identity.domain.User;
import com.newland.erp.identity.domain.UserRoleAssignment;
import com.newland.erp.identity.domain.UserStatus;
import com.newland.erp.identity.application.integration.IdentityAuthorizationPort;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public final class IdentityService implements IdentityAuthorizationPort {
    private static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(15);
    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(30);
    private final IdentityRepository repository;
    private final PasswordHasher passwordHasher;
    private final TokenService tokenService;
    private final IdentityAuditPort audit;
    private final ApplicationEventPublisher events;
    private final PasswordPolicy passwordPolicy;
    private final Clock clock;

    public IdentityService(final IdentityRepository identityRepository, final PasswordHasher hasher,
                           final TokenService tokens, final IdentityAuditPort auditPort,
                           final ApplicationEventPublisher eventPublisher, final Clock systemClock) {
        this.repository = identityRepository;
        this.passwordHasher = hasher;
        this.tokenService = tokens;
        this.audit = auditPort;
        this.events = eventPublisher;
        this.clock = systemClock;
        this.passwordPolicy = PasswordPolicy.enterpriseDefault();
    }

    @Transactional
    public User createUser(final IdentityCommands.CreateUser command, final String actor) {
        passwordPolicy.validate(command.password());
        if (repository.usernameExists(command.username())) {
            throw new IdentityConflictException("Username already exists.");
        }
        final Instant now = now();
        final User user = new User(UUID.randomUUID(), command.username(), command.email(), command.displayName(),
                UserStatus.ACTIVE, 0, null, now.plus(passwordPolicy.passwordTtl()), now, now);
        final User inserted = repository.insertUser(user);
        repository.insertCredential(new PasswordCredential(UUID.randomUUID(), inserted.id(),
                passwordHasher.hash(command.password()), now, user.passwordExpiresAt()));
        publish("UserCreated", inserted.id(), actor, Map.of("username", inserted.username().value()));
        return inserted;
    }

    @Transactional
    public User updateUser(final IdentityCommands.UpdateUser command, final String actor) {
        final User user = user(command.id());
        final User updated = repository.updateUser(new User(user.id(), user.username(), command.email(),
                command.displayName(), user.status(), user.failedLoginAttempts(), user.lockedUntil(),
                user.passwordExpiresAt(), user.createdAt(), now()));
        publish("UserUpdated", updated.id(), actor, Map.of("username", updated.username().value()));
        return updated;
    }

    @Transactional(readOnly = true)
    public List<User> listUsers() {
        return repository.listUsers();
    }

    @Transactional(readOnly = true)
    public User getUser(final UUID id) {
        return user(id);
    }

    @Transactional
    public Role createRole(final IdentityCommands.CreateRole command, final String actor) {
        if (repository.roleCodeExists(command.code())) {
            throw new IdentityConflictException("Role code already exists.");
        }
        final Role role = repository.insertRole(new Role(UUID.randomUUID(), command.code(), command.name(),
                command.description(), false));
        publish("RoleCreated", role.id(), actor, Map.of("code", role.code()));
        return role;
    }

    @Transactional(readOnly = true)
    public List<Role> listRoles() {
        return repository.listRoles();
    }

    @Transactional
    public Permission createPermission(final IdentityCommands.CreatePermission command, final String actor) {
        if (repository.capabilityExists(command.capability())) {
            throw new IdentityConflictException("Capability already exists.");
        }
        final Permission permission = repository.insertPermission(new Permission(UUID.randomUUID(),
                new Capability(command.capability()), command.description()));
        publish("PermissionCreated", permission.id(), actor, Map.of("capability", permission.capability().value()));
        return permission;
    }

    @Transactional(readOnly = true)
    public List<Permission> listPermissions() {
        return repository.listPermissions();
    }

    @Transactional
    public UserRoleAssignment assignRole(final IdentityCommands.AssignRole command, final String actor) {
        user(command.userId());
        role(command.roleId());
        final UserRoleAssignment assignment = repository.insertUserRoleAssignment(new UserRoleAssignment(
                UUID.randomUUID(), command.userId(), command.roleId(), command.scope(), now()));
        publish("UserRoleAssigned", assignment.id(), actor, Map.of("userId", command.userId().toString()));
        return assignment;
    }

    @Transactional
    public RolePermissionAssignment assignPermission(final IdentityCommands.AssignPermission command,
                                                     final String actor) {
        role(command.roleId());
        permission(command.permissionId());
        final RolePermissionAssignment assignment = repository.insertRolePermissionAssignment(
                new RolePermissionAssignment(UUID.randomUUID(), command.roleId(), command.permissionId(), now()));
        publish("RolePermissionAssigned", assignment.id(), actor, Map.of("roleId", command.roleId().toString()));
        return assignment;
    }

    @Transactional(readOnly = true)
    public Set<String> resolveCapabilities(final UUID userId, final OrganizationScope scope) {
        if (user(userId).status() != UserStatus.ACTIVE) {
            return Set.of();
        }
        final Set<String> capabilities = new LinkedHashSet<>();
        for (final UserRoleAssignment roleAssignment : repository.listUserRoleAssignments(userId)) {
            if (roleAssignment.scope().equals(scope)) {
                for (final RolePermissionAssignment permissionAssignment
                        : repository.listRolePermissionAssignments(roleAssignment.roleId())) {
                    capabilities.add(permission(permissionAssignment.permissionId()).capability().value());
                }
            }
        }
        return Set.copyOf(capabilities);
    }

    @Transactional(readOnly = true)
    public AuthorizationDecision decide(final UUID userId, final String capability, final OrganizationScope scope) {
        final boolean granted = resolveCapabilities(userId, scope).contains(capability);
        return new AuthorizationDecision(granted, capability, scope, granted ? "granted" : "missing capability");
    }

    public AuthorizationDecision decideCompany(
            final UUID userId, final String capability, final UUID companyId) {
        return decide(userId, capability, new OrganizationScope(ScopeType.COMPANY, companyId));
    }

    @Override
    public boolean isCompanyCapabilityGranted(
            final UUID userId, final String capability, final UUID companyId) {
        return decideCompany(userId, capability, companyId).granted();
    }

    @Transactional(readOnly = true)
    public boolean hasEnterpriseCapability(final UUID userId, final String capability) {
        if (user(userId).status() != UserStatus.ACTIVE) {
            return false;
        }
        for (final UserRoleAssignment roleAssignment : repository.listUserRoleAssignments(userId)) {
            if (roleAssignment.scope().type() != ScopeType.ENTERPRISE) {
                continue;
            }
            final Role assignedRole = role(roleAssignment.roleId());
            if (!assignedRole.systemRole()) {
                continue;
            }
            for (final RolePermissionAssignment permissionAssignment
                    : repository.listRolePermissionAssignments(roleAssignment.roleId())) {
                if (permission(permissionAssignment.permissionId()).capability().value().equals(capability)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean isSystemEnterpriseCapabilityGranted(
            final UUID userId, final String capability) {
        return hasEnterpriseCapability(userId, capability);
    }

    @Transactional
    public AuthTokens login(final IdentityCommands.Login command) {
        final User user = repository.findUserByUsername(command.username())
                .orElseThrow(() -> new AuthenticationFailedException("Invalid credentials."));
        final PasswordCredential credential = repository.findCurrentCredential(user.id())
                .orElseThrow(() -> new AuthenticationFailedException("Invalid credentials."));
        if (!user.canAuthenticate(now())) {
            throw new AuthenticationFailedException("Invalid credentials.");
        }
        if (!passwordHasher.matches(command.password(), credential.passwordHash())) {
            repository.updateUser(user.recordFailedLogin(passwordPolicy.maxFailedAttempts(),
                    passwordPolicy.lockDuration(), now()));
            throw new AuthenticationFailedException("Invalid credentials.");
        }
        final User authenticated = repository.updateUser(user.recordSuccessfulLogin(now()));
        final Session session = repository.insertSession(new Session(UUID.randomUUID(), authenticated.id(),
                command.deviceLabel(), now(), now().plus(REFRESH_TOKEN_TTL), null));
        final String refreshToken = tokenService.newRefreshToken();
        final Instant accessExpiresAt = now().plus(ACCESS_TOKEN_TTL);
        final Instant refreshExpiresAt = now().plus(command.rememberMe() ? REFRESH_TOKEN_TTL.multipliedBy(3)
                : REFRESH_TOKEN_TTL);
        repository.insertRefreshToken(new RefreshToken(UUID.randomUUID(), session.id(), authenticated.id(),
                tokenService.tokenHash(refreshToken), now(), refreshExpiresAt, null, null));
        final String accessToken = tokenService.issueAccessToken(authenticated, Set.of(), session.id(),
                accessExpiresAt);
        publish("UserLoggedIn", authenticated.id(), authenticated.username().value(), Map.of());
        return new AuthTokens(accessToken, refreshToken, accessExpiresAt, refreshExpiresAt, session.id());
    }

    @Transactional
    public AuthTokens refresh(final String refreshTokenValue) {
        final String hash = tokenService.tokenHash(refreshTokenValue);
        final RefreshToken token = repository.findRefreshTokenByHash(hash)
                .orElseThrow(() -> new AuthenticationFailedException("Invalid refresh token."));
        if (!token.usable(now())) {
            throw new AuthenticationFailedException("Refresh token is expired, rotated, or revoked.");
        }
        final Session session = repository.findSession(token.sessionId())
                .orElseThrow(() -> new AuthenticationFailedException("Invalid refresh token."));
        if (!session.active(now())) {
            throw new AuthenticationFailedException("Refresh token session is expired or revoked.");
        }
        final User user = user(token.userId());
        final String nextRefreshToken = tokenService.newRefreshToken();
        repository.updateRefreshToken(new RefreshToken(token.id(), token.sessionId(), token.userId(),
                token.tokenHash(), token.issuedAt(), token.expiresAt(), now(), token.revokedAt()));
        final Instant accessExpiresAt = now().plus(ACCESS_TOKEN_TTL);
        final Instant refreshExpiresAt = now().plus(REFRESH_TOKEN_TTL);
        repository.insertRefreshToken(new RefreshToken(UUID.randomUUID(), token.sessionId(), user.id(),
                tokenService.tokenHash(nextRefreshToken), now(), refreshExpiresAt, null, null));
        return new AuthTokens(tokenService.issueAccessToken(user, Set.of(), token.sessionId(), accessExpiresAt),
                nextRefreshToken, accessExpiresAt, refreshExpiresAt, token.sessionId());
    }

    @Transactional
    public void changePassword(final IdentityCommands.ChangePassword command, final String actor) {
        passwordPolicy.validate(command.newPassword());
        final PasswordCredential current = repository.findCurrentCredential(command.userId())
                .orElseThrow(() -> new IdentityNotFoundException("Password credential not found."));
        if (!passwordHasher.matches(command.currentPassword(), current.passwordHash())) {
            throw new AuthenticationFailedException("Invalid credentials.");
        }
        repository.archiveCurrentCredential(command.userId());
        repository.insertCredential(new PasswordCredential(UUID.randomUUID(), command.userId(),
                passwordHasher.hash(command.newPassword()), now(), now().plus(passwordPolicy.passwordTtl())));
        publish("PasswordChanged", command.userId(), actor, Map.of());
    }

    @Transactional
    public void revokeSession(final UUID sessionId, final String actor) {
        final Session session = repository.findSession(sessionId)
                .orElseThrow(() -> new IdentityNotFoundException("Session not found."));
        repository.updateSession(new Session(session.id(), session.userId(), session.deviceLabel(),
                session.createdAt(), session.expiresAt(), now()));
        publish("SessionRevoked", session.userId(), actor, Map.of("sessionId", session.id().toString()));
    }

    @Transactional(readOnly = true)
    public List<Session> listSessions(final UUID userId) {
        return repository.listSessions(userId);
    }

    private User user(final UUID id) {
        return repository.findUser(id).orElseThrow(() -> new IdentityNotFoundException("User not found: " + id));
    }

    private Role role(final UUID id) {
        return repository.findRole(id).orElseThrow(() -> new IdentityNotFoundException("Role not found: " + id));
    }

    private Permission permission(final UUID id) {
        return repository.findPermission(id)
                .orElseThrow(() -> new IdentityNotFoundException("Permission not found: " + id));
    }

    private Instant now() {
        return Instant.now(clock);
    }

    private void publish(final String type, final UUID aggregateId, final String actor,
                         final Map<String, String> payload) {
        final IdentityEvent event = new IdentityEvent(UUID.randomUUID(), type, aggregateId, now(), Map.copyOf(payload));
        events.publishEvent(event);
        audit.record(new IdentityAuditEvent(event.eventId(), event.eventType(), event.aggregateId(), actor,
                event.occurredAt()));
    }
}
