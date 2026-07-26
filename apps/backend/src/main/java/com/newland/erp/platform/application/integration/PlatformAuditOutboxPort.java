package com.newland.erp.platform.application.integration;

import java.util.Map;
import java.util.UUID;

public interface PlatformAuditOutboxPort {
  void recordAudit(
      String actor,
      String action,
      String targetType,
      UUID targetId,
      Map<String, String> attributes);

  void publishEvent(
      String sourceContext,
      String eventType,
      UUID aggregateId,
      Map<String, String> payload);

  default void publishEvent(
      UUID eventId,
      String sourceContext,
      String eventType,
      UUID aggregateId,
      Map<String, String> payload) {
    throw new UnsupportedOperationException("Explicit outbox event identifiers are unsupported.");
  }

  default void retryEvent(final UUID eventId) {
    throw new UnsupportedOperationException("Outbox retry is unsupported.");
  }

  void attachFile(String ownerContext, String ownerType, UUID ownerId, UUID fileId);
}
