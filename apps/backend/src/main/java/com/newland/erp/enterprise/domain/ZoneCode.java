package com.newland.erp.enterprise.domain;

public record ZoneCode(String value) {
    public ZoneCode {
        value = TextValue.businessCode(value, "zoneCode");
    }
}
