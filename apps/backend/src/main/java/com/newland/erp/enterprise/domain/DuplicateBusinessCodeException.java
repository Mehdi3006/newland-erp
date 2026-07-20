package com.newland.erp.enterprise.domain;

public final class DuplicateBusinessCodeException extends EnterpriseStructureException {
    public DuplicateBusinessCodeException(final String message) {
        super(message);
    }
}
