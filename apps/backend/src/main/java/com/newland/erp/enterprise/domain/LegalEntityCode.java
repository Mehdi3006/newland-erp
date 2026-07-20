package com.newland.erp.enterprise.domain;

public record LegalEntityCode(String value) {
    public LegalEntityCode {
        value = TextValue.businessCode(value, "legalEntityCode");
    }
}
