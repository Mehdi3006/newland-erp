package com.newland.erp.crm.domain;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

public record Lead(
    UUID id,
    String idempotencyKey,
    UUID companyId,
    UUID branchId,
    UUID ownerId,
    String leadNumber,
    String organizationName,
    String contactName,
    String email,
    String phone,
    String source,
    Status status,
    String dispositionReason,
    long version,
    Instant createdAt,
    Instant updatedAt,
    String actor) {
  public Lead {
    require(id, "lead id");
    require(companyId, "company id");
    require(ownerId, "owner id");
    require(status, "lead status");
    require(createdAt, "created at");
    require(updatedAt, "updated at");
    idempotencyKey = text(idempotencyKey, "idempotency key");
    leadNumber = text(leadNumber, "lead number").toUpperCase(Locale.ROOT);
    organizationName = text(organizationName, "organization name");
    contactName = text(contactName, "contact name");
    source = text(source, "lead source").toUpperCase(Locale.ROOT);
    actor = text(actor, "actor");
    email = optional(email);
    phone = optional(phone);
    dispositionReason = optional(dispositionReason);
    if (email.isBlank() && phone.isBlank()) {
      throw new IllegalArgumentException("Lead requires an email address or phone number.");
    }
    if (version < 0) {
      throw new IllegalArgumentException("Lead version cannot be negative.");
    }
    if ((status == Status.DISQUALIFIED || status == Status.CONVERTED)
        && dispositionReason.isBlank()) {
      throw new IllegalArgumentException("Terminal lead status requires a disposition reason.");
    }
  }

  public Lead qualify(final Instant now) {
    requireStatus(Status.NEW, "Only new leads can be qualified.");
    return copy(Status.QUALIFIED, "", now);
  }

  public Lead disqualify(final String reason, final Instant now) {
    if (status != Status.NEW && status != Status.QUALIFIED) {
      throw new IllegalStateException("Only open leads can be disqualified.");
    }
    return copy(Status.DISQUALIFIED, text(reason, "disqualification reason"), now);
  }

  public Lead convert(final String reason, final Instant now) {
    requireStatus(Status.QUALIFIED, "Only qualified leads can be converted.");
    return copy(Status.CONVERTED, text(reason, "conversion reason"), now);
  }

  private Lead copy(final Status next, final String reason, final Instant now) {
    return new Lead(
        id, idempotencyKey, companyId, branchId, ownerId, leadNumber, organizationName,
        contactName, email, phone, source, next, reason, version + 1, createdAt,
        java.util.Objects.requireNonNull(now), actor);
  }

  private void requireStatus(final Status expected, final String message) {
    if (status != expected) {
      throw new IllegalStateException(message);
    }
  }

  public enum Status {
    NEW,
    QUALIFIED,
    DISQUALIFIED,
    CONVERTED
  }

  static String text(final String value, final String name) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(name + " is required.");
    }
    return value.trim();
  }

  static String optional(final String value) {
    return value == null ? "" : value.trim();
  }

  static void require(final Object value, final String name) {
    if (value == null) {
      throw new IllegalArgumentException(name + " is required.");
    }
  }
}
