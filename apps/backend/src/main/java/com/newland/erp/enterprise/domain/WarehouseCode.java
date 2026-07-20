package com.newland.erp.enterprise.domain;

public record WarehouseCode(String value) {
    public WarehouseCode {
        value = TextValue.businessCode(value, "warehouseCode");
    }
}
