package com.newland.erp.identity.application;

import java.time.Instant;
import java.util.UUID;

public record IdentityAuditEvent(UUID eventId, String eventType, UUID subjectId, String actor, Instant occurredAt) {
}
