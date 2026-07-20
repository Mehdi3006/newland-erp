package com.newland.erp.identity.infrastructure;

import com.newland.erp.identity.application.IdentityRepository;
import com.newland.erp.identity.domain.Capability;
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

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public final class JooqIdentityRepository implements IdentityRepository {
    private final DSLContext dsl;

    public JooqIdentityRepository(final DSLContext dslContext) {
        this.dsl = dslContext;
    }

    @Override
    public boolean usernameExists(final Username username) {
        return exists("iam_user", text("username").eq(username.value()));
    }

    @Override
    public boolean roleCodeExists(final String roleCode) {
        return exists("iam_role", text("code").eq(roleCode.trim().toUpperCase(java.util.Locale.ROOT)));
    }

    @Override
    public boolean capabilityExists(final String capability) {
        return exists("iam_permission", text("capability").eq(capability));
    }

    @Override
    public User insertUser(final User user) {
        dsl.insertInto(table("iam_user"))
                .columns(id(), text("username"), text("email"), text("display_name"), text("status"),
                        integer("failed_login_attempts"), instant("locked_until"), instant("password_expires_at"),
                        instant("created_at"), instant("updated_at"))
                .values(user.id(), user.username().value(), user.email().value(), user.displayName(),
                        user.status().name(), user.failedLoginAttempts(), user.lockedUntil(),
                        user.passwordExpiresAt(), user.createdAt(), user.updatedAt())
                .execute();
        return user;
    }

    @Override
    public User updateUser(final User user) {
        dsl.update(table("iam_user"))
                .set(text("email"), user.email().value())
                .set(text("display_name"), user.displayName())
                .set(text("status"), user.status().name())
                .set(integer("failed_login_attempts"), user.failedLoginAttempts())
                .set(instant("locked_until"), user.lockedUntil())
                .set(instant("password_expires_at"), user.passwordExpiresAt())
                .set(instant("updated_at"), user.updatedAt())
                .where(id().eq(user.id()))
                .execute();
        return user;
    }

    @Override
    public Optional<User> findUser(final UUID id) {
        return dsl.selectFrom(table("iam_user")).where(id().eq(id)).fetchOptional(this::user);
    }

    @Override
    public Optional<User> findUserByUsername(final Username username) {
        return dsl.selectFrom(table("iam_user")).where(text("username").eq(username.value())).fetchOptional(this::user);
    }

    @Override
    public List<User> listUsers() {
        return dsl.selectFrom(table("iam_user")).orderBy(text("username")).fetch(this::user);
    }

    @Override
    public PasswordCredential insertCredential(final PasswordCredential credential) {
        dsl.insertInto(table("iam_password_credential"))
                .columns(id(), uuid("user_id"), text("password_hash"), instant("changed_at"),
                        instant("expires_at"), bool("current_credential"))
                .values(credential.id(), credential.userId(), credential.passwordHash(), credential.changedAt(),
                        credential.expiresAt(), true)
                .execute();
        return credential;
    }

    @Override
    public Optional<PasswordCredential> findCurrentCredential(final UUID userId) {
        return dsl.selectFrom(table("iam_password_credential"))
                .where(uuid("user_id").eq(userId).and(bool("current_credential").isTrue()))
                .fetchOptional(this::credential);
    }

    @Override
    public void archiveCurrentCredential(final UUID userId) {
        dsl.update(table("iam_password_credential"))
                .set(bool("current_credential"), false)
                .where(uuid("user_id").eq(userId))
                .execute();
    }

    @Override
    public Role insertRole(final Role role) {
        dsl.insertInto(table("iam_role"))
                .columns(id(), text("code"), text("name"), text("description"), bool("system_role"))
                .values(role.id(), role.code(), role.name(), role.description(), role.systemRole())
                .execute();
        return role;
    }

    @Override
    public Role updateRole(final Role role) {
        dsl.update(table("iam_role"))
                .set(text("name"), role.name())
                .set(text("description"), role.description())
                .where(id().eq(role.id()))
                .execute();
        return role;
    }

    @Override
    public Optional<Role> findRole(final UUID id) {
        return dsl.selectFrom(table("iam_role")).where(id().eq(id)).fetchOptional(this::role);
    }

    @Override
    public List<Role> listRoles() {
        return dsl.selectFrom(table("iam_role")).orderBy(text("code")).fetch(this::role);
    }

    @Override
    public Permission insertPermission(final Permission permission) {
        dsl.insertInto(table("iam_permission"))
                .columns(id(), text("capability"), text("description"))
                .values(permission.id(), permission.capability().value(), permission.description())
                .execute();
        return permission;
    }

    @Override
    public Optional<Permission> findPermission(final UUID id) {
        return dsl.selectFrom(table("iam_permission")).where(id().eq(id)).fetchOptional(this::permission);
    }

    @Override
    public Optional<Permission> findPermissionByCapability(final String capability) {
        return dsl.selectFrom(table("iam_permission"))
                .where(text("capability").eq(capability))
                .fetchOptional(this::permission);
    }

    @Override
    public List<Permission> listPermissions() {
        return dsl.selectFrom(table("iam_permission")).orderBy(text("capability")).fetch(this::permission);
    }

    @Override
    public UserRoleAssignment insertUserRoleAssignment(final UserRoleAssignment assignment) {
        dsl.insertInto(table("iam_user_role_assignment"))
                .columns(id(), uuid("user_id"), uuid("role_id"), text("scope_type"), uuid("scope_id"),
                        instant("assigned_at"))
                .values(assignment.id(), assignment.userId(), assignment.roleId(), assignment.scope().type().name(),
                        assignment.scope().scopeId(), assignment.assignedAt())
                .execute();
        return assignment;
    }

    @Override
    public void removeUserRoleAssignment(final UUID assignmentId) {
        dsl.deleteFrom(table("iam_user_role_assignment")).where(id().eq(assignmentId)).execute();
    }

    @Override
    public List<UserRoleAssignment> listUserRoleAssignments(final UUID userId) {
        return dsl.selectFrom(table("iam_user_role_assignment"))
                .where(uuid("user_id").eq(userId))
                .fetch(this::userRoleAssignment);
    }

    @Override
    public RolePermissionAssignment insertRolePermissionAssignment(final RolePermissionAssignment assignment) {
        dsl.insertInto(table("iam_role_permission_assignment"))
                .columns(id(), uuid("role_id"), uuid("permission_id"), instant("assigned_at"))
                .values(assignment.id(), assignment.roleId(), assignment.permissionId(), assignment.assignedAt())
                .execute();
        return assignment;
    }

    @Override
    public List<RolePermissionAssignment> listRolePermissionAssignments(final UUID roleId) {
        return dsl.selectFrom(table("iam_role_permission_assignment"))
                .where(uuid("role_id").eq(roleId))
                .fetch(this::rolePermissionAssignment);
    }

    @Override
    public Session insertSession(final Session session) {
        dsl.insertInto(table("iam_session"))
                .columns(id(), uuid("user_id"), text("device_label"), instant("created_at"), instant("expires_at"),
                        instant("revoked_at"))
                .values(session.id(), session.userId(), session.deviceLabel(), session.createdAt(),
                        session.expiresAt(), session.revokedAt())
                .execute();
        return session;
    }

    @Override
    public Session updateSession(final Session session) {
        dsl.update(table("iam_session"))
                .set(instant("revoked_at"), session.revokedAt())
                .where(id().eq(session.id()))
                .execute();
        return session;
    }

    @Override
    public Optional<Session> findSession(final UUID id) {
        return dsl.selectFrom(table("iam_session")).where(id().eq(id)).fetchOptional(this::session);
    }

    @Override
    public List<Session> listSessions(final UUID userId) {
        return dsl.selectFrom(table("iam_session")).where(uuid("user_id").eq(userId)).fetch(this::session);
    }

    @Override
    public RefreshToken insertRefreshToken(final RefreshToken token) {
        dsl.insertInto(table("iam_refresh_token"))
                .columns(id(), uuid("session_id"), uuid("user_id"), text("token_hash"), instant("issued_at"),
                        instant("expires_at"), instant("rotated_at"), instant("revoked_at"))
                .values(token.id(), token.sessionId(), token.userId(), token.tokenHash(), token.issuedAt(),
                        token.expiresAt(), token.rotatedAt(), token.revokedAt())
                .execute();
        return token;
    }

    @Override
    public RefreshToken updateRefreshToken(final RefreshToken token) {
        dsl.update(table("iam_refresh_token"))
                .set(instant("rotated_at"), token.rotatedAt())
                .set(instant("revoked_at"), token.revokedAt())
                .where(id().eq(token.id()))
                .execute();
        return token;
    }

    @Override
    public Optional<RefreshToken> findRefreshTokenByHash(final String tokenHash) {
        return dsl.selectFrom(table("iam_refresh_token"))
                .where(text("token_hash").eq(tokenHash))
                .fetchOptional(this::refreshToken);
    }

    private User user(final Record record) {
        return new User(record.get(id()), new Username(record.get(text("username"))),
                new EmailAddress(record.get(text("email"))), record.get(text("display_name")),
                UserStatus.valueOf(record.get(text("status"))), record.get(integer("failed_login_attempts")),
                auditInstant(record, "locked_until"), auditInstant(record, "password_expires_at"),
                auditInstant(record, "created_at"), auditInstant(record, "updated_at"));
    }

    private PasswordCredential credential(final Record record) {
        return new PasswordCredential(record.get(id()), record.get(uuid("user_id")), record.get(text("password_hash")),
                auditInstant(record, "changed_at"), auditInstant(record, "expires_at"));
    }

    private Role role(final Record record) {
        return new Role(record.get(id()), record.get(text("code")), record.get(text("name")),
                record.get(text("description")), Boolean.TRUE.equals(record.get(bool("system_role"))));
    }

    private Permission permission(final Record record) {
        return new Permission(record.get(id()), new Capability(record.get(text("capability"))),
                record.get(text("description")));
    }

    private UserRoleAssignment userRoleAssignment(final Record record) {
        return new UserRoleAssignment(record.get(id()), record.get(uuid("user_id")), record.get(uuid("role_id")),
                new OrganizationScope(ScopeType.valueOf(record.get(text("scope_type"))), record.get(uuid("scope_id"))),
                auditInstant(record, "assigned_at"));
    }

    private RolePermissionAssignment rolePermissionAssignment(final Record record) {
        return new RolePermissionAssignment(record.get(id()), record.get(uuid("role_id")),
                record.get(uuid("permission_id")), auditInstant(record, "assigned_at"));
    }

    private Session session(final Record record) {
        return new Session(record.get(id()), record.get(uuid("user_id")), record.get(text("device_label")),
                auditInstant(record, "created_at"), auditInstant(record, "expires_at"),
                auditInstant(record, "revoked_at"));
    }

    private RefreshToken refreshToken(final Record record) {
        return new RefreshToken(record.get(id()), record.get(uuid("session_id")), record.get(uuid("user_id")),
                record.get(text("token_hash")), auditInstant(record, "issued_at"), auditInstant(record, "expires_at"),
                auditInstant(record, "rotated_at"), auditInstant(record, "revoked_at"));
    }

    private boolean exists(final String tableName, final org.jooq.Condition condition) {
        return dsl.fetchExists(dsl.selectOne().from(table(tableName)).where(condition));
    }

    private static Table<Record> table(final String name) {
        return DSL.table(DSL.name(name));
    }

    private static Field<UUID> id() {
        return uuid("id");
    }

    private static Field<UUID> uuid(final String name) {
        return DSL.field(DSL.name(name), UUID.class);
    }

    private static Field<String> text(final String name) {
        return DSL.field(DSL.name(name), String.class);
    }

    private static Field<Integer> integer(final String name) {
        return DSL.field(DSL.name(name), Integer.class);
    }

    private static Field<Boolean> bool(final String name) {
        return DSL.field(DSL.name(name), Boolean.class);
    }

    private static Field<Instant> instant(final String name) {
        return DSL.field(DSL.name(name), Instant.class);
    }

    private static Instant auditInstant(final Record record, final String name) {
        final Object value = record.get(DSL.field(DSL.name(name)));
        if (value == null) {
            return null;
        }
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime.toInstant();
        }
        throw new IllegalStateException("Unsupported timestamp value for " + name + ": "
                + value.getClass().getName());
    }
}
