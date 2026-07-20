package com.newland.erp.enterprise.domain;

public final class ReferencedByActiveChildrenException extends EnterpriseStructureException {
    public ReferencedByActiveChildrenException(final String message) {
        super(message);
    }
}
