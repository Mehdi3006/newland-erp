package com.newland.erp.enterprise.infrastructure;

import com.newland.erp.enterprise.application.AuthorizationPort;
import com.newland.erp.enterprise.domain.AuthorizationDeniedException;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public final class HeaderAuthorizationPort implements AuthorizationPort {
    static final String PERMISSIONS_HEADER = "X-Newland-Permissions";

    @Override
    public void require(final String permission) {
        final ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new AuthorizationDeniedException(permission);
        }
        final HttpServletRequest request = attributes.getRequest();
        final String rawPermissions = request.getHeader(PERMISSIONS_HEADER);
        if (rawPermissions == null || rawPermissions.isBlank()) {
            throw new AuthorizationDeniedException(permission);
        }
        final Set<String> permissions = Arrays.stream(rawPermissions.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toUnmodifiableSet());
        if (!permissions.contains(permission)) {
            throw new AuthorizationDeniedException(permission);
        }
    }
}
