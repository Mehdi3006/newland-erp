package com.newland.erp.enterprise.domain;

import java.time.ZoneId;

public record TimeZoneId(String value) {
    public TimeZoneId {
        value = TextValue.required(value, "timeZoneId", 64);
        ZoneId.of(value);
    }
}
