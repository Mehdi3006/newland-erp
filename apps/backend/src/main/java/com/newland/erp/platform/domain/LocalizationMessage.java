package com.newland.erp.platform.domain;

public record LocalizationMessage(String locale, String messageKey, String message) {
    public LocalizationMessage {
        PlatformDomainEvent.requireText(locale, "locale");
        PlatformDomainEvent.requireText(messageKey, "message key");
        PlatformDomainEvent.requireText(message, "message");
    }
}
