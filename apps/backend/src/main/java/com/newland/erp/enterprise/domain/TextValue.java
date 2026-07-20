package com.newland.erp.enterprise.domain;

import java.util.Locale;
import java.util.regex.Pattern;

final class TextValue {
    private static final Pattern BUSINESS_CODE = Pattern.compile("[A-Z0-9][A-Z0-9_-]{1,31}");
    private static final Pattern COUNTRY_CODE = Pattern.compile("[A-Z]{2}");
    private static final Pattern CURRENCY_CODE = Pattern.compile("[A-Z]{3}");

    private TextValue() {
    }

    static String required(final String rawValue, final String field, final int maxLength) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new IllegalArgumentException(field + " is required.");
        }
        final String normalizedValue = rawValue.trim();
        if (normalizedValue.length() > maxLength) {
            throw new IllegalArgumentException(field + " must be at most " + maxLength + " characters.");
        }
        return normalizedValue;
    }

    static String businessCode(final String rawValue, final String field) {
        final String normalizedValue = required(rawValue, field, 32).toUpperCase(Locale.ROOT);
        if (!BUSINESS_CODE.matcher(normalizedValue).matches()) {
            throw new IllegalArgumentException(field + " must use 2-32 characters: A-Z, 0-9, underscore, hyphen.");
        }
        return normalizedValue;
    }

    static String countryCode(final String rawValue) {
        final String normalizedValue = required(rawValue, "countryCode", 2).toUpperCase(Locale.ROOT);
        if (!COUNTRY_CODE.matcher(normalizedValue).matches()) {
            throw new IllegalArgumentException("countryCode must be ISO-3166 alpha-2 style.");
        }
        return normalizedValue;
    }

    static String currencyCode(final String rawValue) {
        final String normalizedValue = required(rawValue, "currencyCode", 3).toUpperCase(Locale.ROOT);
        if (!CURRENCY_CODE.matcher(normalizedValue).matches()) {
            throw new IllegalArgumentException("currencyCode must be ISO-4217 style.");
        }
        return normalizedValue;
    }
}
