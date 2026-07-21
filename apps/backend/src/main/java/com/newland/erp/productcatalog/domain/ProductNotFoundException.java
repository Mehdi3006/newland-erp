package com.newland.erp.productcatalog.domain;

public final class ProductNotFoundException extends ProductException {
    public ProductNotFoundException(final String message) {
        super(message);
    }
}
