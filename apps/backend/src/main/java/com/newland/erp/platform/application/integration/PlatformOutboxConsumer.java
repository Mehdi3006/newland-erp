package com.newland.erp.platform.application.integration;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Published extension point for durable, post-commit outbox consumers. */
public interface PlatformOutboxConsumer {
  boolean supports(String sourceContext, String eventType);

  void consume(OutboxEvent event);

  record OutboxEvent(
      UUID eventId,
      String sourceContext,
      String eventType,
      UUID aggregateId,
      Instant occurredAt,
      Map<String, String> payload) {}
}
