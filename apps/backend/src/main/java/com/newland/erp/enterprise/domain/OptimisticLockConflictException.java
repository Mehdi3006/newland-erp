package com.newland.erp.enterprise.domain;

public final class OptimisticLockConflictException extends EnterpriseStructureException {
    public OptimisticLockConflictException(final String message) {
        super(message);
    }
}
