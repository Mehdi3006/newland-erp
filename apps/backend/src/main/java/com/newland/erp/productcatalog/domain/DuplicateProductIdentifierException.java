package com.newland.erp.productcatalog.domain;

public final class DuplicateProductIdentifierException extends ProductException {
    public DuplicateProductIdentifierException(final String message) {
        super(message);
    }
}
