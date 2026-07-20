package com.newland.erp.enterprise.domain;

public final class AuthorizationDeniedException extends EnterpriseStructureException {
    public AuthorizationDeniedException(final String permission) {
        super("Missing required permission: " + permission);
    }
}
