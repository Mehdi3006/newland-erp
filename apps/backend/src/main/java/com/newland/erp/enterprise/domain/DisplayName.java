package com.newland.erp.enterprise.domain;

public record DisplayName(String value) {
    public DisplayName {
        value = TextValue.required(value, "displayName", 160);
    }
}
