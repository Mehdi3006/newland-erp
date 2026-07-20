package com.newland.erp.enterprise.domain;

import java.util.Map;
import java.util.TreeMap;

public record LocalizedName(Map<String, String> values) {
    public LocalizedName {
        if (values == null || values.isEmpty()) {
            values = Map.of();
        } else {
            final Map<String, String> normalizedValues = new TreeMap<>();
            values.forEach((locale, name) -> {
                final String normalizedLocale = TextValue.required(locale, "localizedName.locale", 16).toLowerCase();
                final String normalizedName = TextValue.required(name, "localizedName.value", 160);
                normalizedValues.put(normalizedLocale, normalizedName);
            });
            values = Map.copyOf(normalizedValues);
        }
    }
}
