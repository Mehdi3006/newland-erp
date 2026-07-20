package com.newland.erp.identity.domain;

public final class AccessDeniedException extends IdentityException {
    public AccessDeniedException(final String message) {
        super(message);
    }
}
