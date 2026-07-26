package com.newland.erp.crm.domain;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

public record Activity(
    UUID id,
    String idempotencyKey,
    UUID companyId,
    UUID customerId,
    UUID leadId,
    UUID opportunityId,
    Type type,
    String subject,
    String details,
    Instant occurredAt,
    Instant followUpAt,
    String actor) {
  public Activity {
    Lead.require(id, "activity id");
    Lead.require(companyId, "company id");
    Lead.require(type, "activity type");
    Lead.require(occurredAt, "activity time");
    idempotencyKey = Lead.text(idempotencyKey, "idempotency key");
    subject = Lead.text(subject, "activity subject");
    details = Lead.optional(details);
    actor = Lead.text(actor, "actor");
    if (customerId == null && leadId == null && opportunityId == null) {
      throw new IllegalArgumentException(
          "Activity must reference a customer, lead, or opportunity.");
    }
    if (followUpAt != null && followUpAt.isBefore(occurredAt)) {
      throw new IllegalArgumentException("Activity follow-up cannot precede occurrence.");
    }
  }

  public enum Type {
    CALL,
    EMAIL,
    MEETING,
    NOTE,
    TASK;

    public static Type from(final String value) {
      return valueOf(Lead.text(value, "activity type").toUpperCase(Locale.ROOT));
    }
  }
}
