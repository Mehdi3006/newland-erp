package com.newland.erp.enterprise.infrastructure;

import com.newland.erp.enterprise.application.AuditPort;
import com.newland.erp.enterprise.application.EnterpriseStructureAuditEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public final class LoggingAuditPort implements AuditPort {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingAuditPort.class);

    @Override
    public void record(final EnterpriseStructureAuditEvent event) {
        LOGGER.info(
                "enterprise_structure_audit eventId={} eventType={} aggregateId={} actor={} correlationId={} "
                        + "occurredAt={}",
                event.eventId(),
                event.eventType(),
                event.aggregateId(),
                event.actor(),
                event.correlationId(),
                event.occurredAt()
        );
    }
}
