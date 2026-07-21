package com.newland.erp.inventory.domain;

public final class InventoryNotFoundException extends InventoryException {
    public InventoryNotFoundException(final String message) {
        super(message);
    }
}
