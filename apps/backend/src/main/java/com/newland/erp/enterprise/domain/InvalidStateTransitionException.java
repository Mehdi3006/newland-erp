package com.newland.erp.enterprise.domain;

public final class InvalidStateTransitionException extends EnterpriseStructureException {
    public InvalidStateTransitionException(final String message) {
        super(message);
    }
}
