package com.newland.erp.identity.application;

public interface IdentityAuditPort {
    void record(IdentityAuditEvent event);
}
