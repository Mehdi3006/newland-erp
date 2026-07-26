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

  void attachFile(String ownerContext, String ownerType, UUID ownerId, UUID fileId);
}
