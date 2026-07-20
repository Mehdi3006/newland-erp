package com.newland.erp.identity.infrastructure;

import com.newland.erp.identity.application.IdentityAuditEvent;
import com.newland.erp.identity.application.IdentityAuditPort;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public final class LoggingIdentityAuditPort implements IdentityAuditPort {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingIdentityAuditPort.class);

    @Override
    public void record(final IdentityAuditEvent event) {
        LOGGER.info("identity_audit eventId={} eventType={} subjectId={} actor={}",
                event.eventId(), event.eventType(), event.subjectId(), event.actor());
    }
}
