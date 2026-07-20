package com.newland.erp.enterprise.domain;

public record CurrencyCode(String value) {
    public CurrencyCode {
        value = TextValue.currencyCode(value);
    }
}
