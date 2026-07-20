package com.newland.erp.enterprise.infrastructure;

import com.newland.erp.enterprise.application.AuditPort;
import com.newland.erp.enterprise.application.EnterpriseStructureAuditEvent;

import org.springframework.stereotype.Component;

@Component
public final class NoOpAuditPort implements AuditPort {
    @Override
    public void record(final EnterpriseStructureAuditEvent event) {
        // Future Audit bounded context will implement this port. P3.1 records audit metadata and publishes events.
    }
}
