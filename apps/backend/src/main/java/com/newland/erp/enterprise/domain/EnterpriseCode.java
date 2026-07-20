package com.newland.erp.enterprise.domain;

public record EnterpriseCode(String value) {
    public EnterpriseCode {
        value = TextValue.businessCode(value, "enterpriseCode");
    }
}
