package com.newland.erp.identity.application;

import com.newland.erp.identity.domain.PasswordCredential;
import com.newland.erp.identity.domain.Permission;
import com.newland.erp.identity.domain.RefreshToken;
import com.newland.erp.identity.domain.Role;
import com.newland.erp.identity.domain.RolePermissionAssignment;
import com.newland.erp.identity.domain.Session;
import com.newland.erp.identity.domain.User;
import com.newland.erp.identity.domain.UserRoleAssignment;
import com.newland.erp.identity.domain.Username;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IdentityRepository {
    boolean usernameExists(Username username);

    boolean roleCodeExists(String roleCode);

    boolean capabilityExists(String capability);

    User insertUser(User user);

    User updateUser(User user);

    Optional<User> findUser(UUID id);

    Optional<User> findUserByUsername(Username username);

    List<User> listUsers();

    PasswordCredential insertCredential(PasswordCredential credential);

    Optional<PasswordCredential> findCurrentCredential(UUID userId);

    void archiveCurrentCredential(UUID userId);

    Role insertRole(Role role);

    Role updateRole(Role role);

    Optional<Role> findRole(UUID id);

    List<Role> listRoles();

    Permission insertPermission(Permission permission);

    Optional<Permission> findPermission(UUID id);

    Optional<Permission> findPermissionByCapability(String capability);

    List<Permission> listPermissions();

    UserRoleAssignment insertUserRoleAssignment(UserRoleAssignment assignment);

    void removeUserRoleAssignment(UUID assignmentId);

    List<UserRoleAssignment> listUserRoleAssignments(UUID userId);

    RolePermissionAssignment insertRolePermissionAssignment(RolePermissionAssignment assignment);

    List<RolePermissionAssignment> listRolePermissionAssignments(UUID roleId);

    Session insertSession(Session session);

    Session updateSession(Session session);

    Optional<Session> findSession(UUID id);

    List<Session> listSessions(UUID userId);

    RefreshToken insertRefreshToken(RefreshToken token);

    RefreshToken updateRefreshToken(RefreshToken token);

    Optional<RefreshToken> findRefreshTokenByHash(String tokenHash);
}
