package com.newland.erp.enterprise.application;

public interface AuditPort {
    void record(EnterpriseStructureAuditEvent event);
}
