package com.newland.erp.enterprise.domain;

public record CompanyCode(String value) {
    public CompanyCode {
        value = TextValue.businessCode(value, "companyCode");
    }
}
