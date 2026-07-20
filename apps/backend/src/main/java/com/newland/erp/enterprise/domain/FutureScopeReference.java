package com.newland.erp.enterprise.domain;

public record FutureScopeReference(String type, String reference) {
    public FutureScopeReference {
        type = TextValue.businessCode(type, "futureScopeReference.type");
        reference = TextValue.required(reference, "futureScopeReference.reference", 80);
    }
}
