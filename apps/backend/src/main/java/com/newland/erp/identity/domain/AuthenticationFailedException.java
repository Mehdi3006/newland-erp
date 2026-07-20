package com.newland.erp.identity.domain;

public final class AuthenticationFailedException extends IdentityException {
    public AuthenticationFailedException(final String message) {
        super(message);
    }
}
