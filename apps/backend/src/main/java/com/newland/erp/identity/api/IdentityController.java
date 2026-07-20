package com.newland.erp.identity.api;

import com.newland.erp.identity.application.IdentityCommands;
import com.newland.erp.identity.application.IdentityService;
import com.newland.erp.identity.domain.EmailAddress;
import com.newland.erp.identity.domain.Username;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping
public final class IdentityController {
    private static final String ACTOR_HEADER = "X-Newland-Actor";
    private final IdentityService service;

    public IdentityController(final IdentityService identityService) {
        this.service = identityService;
    }

    @PostMapping("/api/v1/auth/login")
    public IdentityDtos.TokenResponse login(@Valid @RequestBody final IdentityDtos.LoginRequest request) {
        return IdentityDtos.TokenResponse.from(service.login(new IdentityCommands.Login(
                new Username(request.username()), request.password(), request.deviceLabel(), request.rememberMe())));
    }

    @PostMapping("/api/v1/auth/refresh")
    public IdentityDtos.TokenResponse refresh(@Valid @RequestBody final IdentityDtos.RefreshRequest request) {
        return IdentityDtos.TokenResponse.from(service.refresh(request.refreshToken()));
    }

    @PostMapping("/api/v1/auth/logout/{sessionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@PathVariable final UUID sessionId,
                       @RequestHeader(name = ACTOR_HEADER, defaultValue = "system") final String actor) {
        service.revokeSession(sessionId, actor);
    }

    @GetMapping("/api/v1/identity/users")
    public List<IdentityDtos.UserResponse> listUsers() {
        return service.listUsers().stream().map(IdentityDtos.UserResponse::from).toList();
    }

    @PostMapping("/api/v1/identity/users")
    @ResponseStatus(HttpStatus.CREATED)
    public IdentityDtos.UserResponse createUser(@Valid @RequestBody final IdentityDtos.CreateUserRequest request,
                                                @RequestHeader(name = ACTOR_HEADER,
                                                        defaultValue = "system") final String actor) {
        return IdentityDtos.UserResponse.from(service.createUser(new IdentityCommands.CreateUser(
                new Username(request.username()), new EmailAddress(request.email()), request.displayName(),
                request.password()), actor));
    }

    @GetMapping("/api/v1/identity/users/{userId}")
    public IdentityDtos.UserResponse getUser(@PathVariable final UUID userId) {
        return IdentityDtos.UserResponse.from(service.getUser(userId));
    }

    @PutMapping("/api/v1/identity/users/{userId}")
    public IdentityDtos.UserResponse updateUser(@PathVariable final UUID userId,
                                                @Valid @RequestBody final IdentityDtos.UpdateUserRequest request,
                                                @RequestHeader(name = ACTOR_HEADER,
                                                        defaultValue = "system") final String actor) {
        return IdentityDtos.UserResponse.from(service.updateUser(new IdentityCommands.UpdateUser(userId,
                new EmailAddress(request.email()), request.displayName()), actor));
    }

    @PostMapping("/api/v1/identity/users/{userId}/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@PathVariable final UUID userId,
                               @Valid @RequestBody final IdentityDtos.ChangePasswordRequest request,
                               @RequestHeader(name = ACTOR_HEADER, defaultValue = "system") final String actor) {
        service.changePassword(new IdentityCommands.ChangePassword(userId, request.currentPassword(),
                request.newPassword()), actor);
    }

    @GetMapping("/api/v1/identity/users/{userId}/sessions")
    public List<IdentityDtos.SessionResponse> listSessions(@PathVariable final UUID userId) {
        return service.listSessions(userId).stream().map(IdentityDtos.SessionResponse::from).toList();
    }

    @GetMapping("/api/v1/access-control/roles")
    public List<IdentityDtos.RoleResponse> listRoles() {
        return service.listRoles().stream().map(IdentityDtos.RoleResponse::from).toList();
    }

    @PostMapping("/api/v1/access-control/roles")
    @ResponseStatus(HttpStatus.CREATED)
    public IdentityDtos.RoleResponse createRole(@Valid @RequestBody final IdentityDtos.CreateRoleRequest request,
                                                @RequestHeader(name = ACTOR_HEADER,
                                                        defaultValue = "system") final String actor) {
        return IdentityDtos.RoleResponse.from(service.createRole(new IdentityCommands.CreateRole(request.code(),
                request.name(), request.description()), actor));
    }

    @GetMapping("/api/v1/access-control/permissions")
    public List<IdentityDtos.PermissionResponse> listPermissions() {
        return service.listPermissions().stream().map(IdentityDtos.PermissionResponse::from).toList();
    }

    @PostMapping("/api/v1/access-control/permissions")
    @ResponseStatus(HttpStatus.CREATED)
    public IdentityDtos.PermissionResponse createPermission(
            @Valid @RequestBody final IdentityDtos.CreatePermissionRequest request,
            @RequestHeader(name = ACTOR_HEADER, defaultValue = "system") final String actor
    ) {
        return IdentityDtos.PermissionResponse.from(service.createPermission(
                new IdentityCommands.CreatePermission(request.capability(), request.description()), actor));
    }

    @PostMapping("/api/v1/access-control/assignments/user-roles")
    @ResponseStatus(HttpStatus.CREATED)
    public void assignRole(@Valid @RequestBody final IdentityDtos.AssignRoleRequest request,
                           @RequestHeader(name = ACTOR_HEADER, defaultValue = "system") final String actor) {
        service.assignRole(new IdentityCommands.AssignRole(request.userId(), request.roleId(),
                request.scope().toDomain()), actor);
    }

    @PostMapping("/api/v1/access-control/assignments/role-permissions")
    @ResponseStatus(HttpStatus.CREATED)
    public void assignPermission(@Valid @RequestBody final IdentityDtos.AssignPermissionRequest request,
                                 @RequestHeader(name = ACTOR_HEADER, defaultValue = "system") final String actor) {
        service.assignPermission(new IdentityCommands.AssignPermission(request.roleId(), request.permissionId()),
                actor);
    }

    @PostMapping("/api/v1/access-control/decisions")
    public IdentityDtos.AuthorizationResponse decide(
            @Valid @RequestBody final IdentityDtos.AuthorizationRequest request
    ) {
        return IdentityDtos.AuthorizationResponse.from(service.decide(request.userId(), request.capability(),
                request.scope().toDomain()));
    }
}
