package com.newland.erp.enterprise.domain;

public record CountryCode(String value) {
    public CountryCode {
        value = TextValue.countryCode(value);
    }
}
