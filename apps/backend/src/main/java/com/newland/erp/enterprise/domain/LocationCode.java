package com.newland.erp.enterprise.domain;

public record LocationCode(String value) {
    public LocationCode {
        value = TextValue.businessCode(value, "locationCode");
    }
}
