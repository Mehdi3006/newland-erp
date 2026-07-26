package com.newland.erp.servicewarranty.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

public record ServiceTicket(
    UUID id,
    String idempotencyKey,
    String ticketNumber,
    UUID companyId,
    UUID branchId,
    UUID customerId,
    UUID productId,
    UUID skuId,
    String serialCode,
    UUID salesOrderId,
    LocalDate purchaseDate,
    String issueSummary,
    Status status,
    WarrantyDecision warrantyDecision,
    Diagnosis diagnosis,
    Resolution resolution,
    long version,
    Instant createdAt,
    Instant updatedAt,
    String actor) {
  public ServiceTicket {
    WarrantyPolicy.required(id, "ticket id");
    WarrantyPolicy.required(companyId, "company id");
    WarrantyPolicy.required(customerId, "customer id");
    WarrantyPolicy.required(productId, "product id");
    WarrantyPolicy.required(skuId, "SKU id");
    WarrantyPolicy.required(status, "ticket status");
    WarrantyPolicy.required(createdAt, "created at");
    WarrantyPolicy.required(updatedAt, "updated at");
    idempotencyKey = text(idempotencyKey, "idempotency key");
    ticketNumber = text(ticketNumber, "ticket number").toUpperCase(Locale.ROOT);
    issueSummary = text(issueSummary, "issue summary");
    serialCode = optional(serialCode).toUpperCase(Locale.ROOT);
    actor = text(actor, "actor");
    if (version < 0) {
      throw new IllegalArgumentException("Ticket version cannot be negative.");
    }
    if (status == Status.CLOSED && resolution == null) {
      throw new IllegalArgumentException("Closed ticket requires a resolution.");
    }
  }

  public ServiceTicket beginValidation(final Instant now) {
    requireStatus(Status.OPEN, "Only open tickets can begin warranty validation.");
    return copy(Status.VALIDATING, null, diagnosis, resolution, now);
  }

  public ServiceTicket recordWarranty(
      final WarrantyDecision decision, final Instant now) {
    requireStatus(Status.VALIDATING, "Ticket is not awaiting warranty validation.");
    return copy(
        decision.eligible() ? Status.WARRANTY_VALID : Status.WARRANTY_REJECTED,
        decision, diagnosis, resolution, now);
  }

  public ServiceTicket diagnose(final Diagnosis result, final Instant now) {
    if (status != Status.WARRANTY_VALID && status != Status.WARRANTY_REJECTED) {
      throw new IllegalStateException("Warranty decision is required before diagnosis.");
    }
    return copy(Status.AWAITING_APPROVAL, warrantyDecision, result, resolution, now);
  }

  public ServiceTicket approveResolution(
      final Resolution.Type type, final String notes, final Instant now) {
    requireStatus(Status.AWAITING_APPROVAL, "Ticket is not awaiting resolution approval.");
    final Resolution result =
        new Resolution(UUID.randomUUID(), type, text(notes, "resolution notes"), now);
    return copy(
        type == Resolution.Type.REPAIR ? Status.REPAIRING : Status.REPLACING,
        warrantyDecision, diagnosis, result, now);
  }

  public ServiceTicket close(final String outcome, final Instant now) {
    if (status != Status.REPAIRING && status != Status.REPLACING) {
      throw new IllegalStateException("Only resolved work can close a service ticket.");
    }
    final Resolution completed =
        new Resolution(
            resolution.id(), resolution.type(), text(outcome, "closure outcome"),
            java.util.Objects.requireNonNull(now));
    return copy(Status.CLOSED, warrantyDecision, diagnosis, completed, now);
  }

  public ServiceTicket cancel(final String reason, final Instant now) {
    requireStatus(Status.OPEN, "Only open tickets can be cancelled.");
    final Resolution cancelled =
        new Resolution(UUID.randomUUID(), Resolution.Type.CANCELLED, text(reason, "reason"), now);
    return copy(Status.CANCELLED, warrantyDecision, diagnosis, cancelled, now);
  }

  private ServiceTicket copy(
      final Status next,
      final WarrantyDecision decision,
      final Diagnosis nextDiagnosis,
      final Resolution nextResolution,
      final Instant now) {
    return new ServiceTicket(
        id, idempotencyKey, ticketNumber, companyId, branchId, customerId, productId, skuId,
        serialCode, salesOrderId, purchaseDate, issueSummary, next, decision, nextDiagnosis,
        nextResolution, version + 1, createdAt, java.util.Objects.requireNonNull(now), actor);
  }

  private void requireStatus(final Status expected, final String message) {
    if (status != expected) {
      throw new IllegalStateException(message);
    }
  }

  public enum Status {
    OPEN,
    VALIDATING,
    WARRANTY_VALID,
    WARRANTY_REJECTED,
    AWAITING_APPROVAL,
    REPAIRING,
    REPLACING,
    CLOSED,
    CANCELLED
  }

  public record WarrantyDecision(
      UUID id,
      UUID policyId,
      boolean eligible,
      String reason,
      LocalDate coverageEndsOn,
      Instant decidedAt,
      String actor) {
    public WarrantyDecision {
      WarrantyPolicy.required(id, "warranty decision id");
      WarrantyPolicy.required(policyId, "warranty policy id");
      WarrantyPolicy.required(decidedAt, "decision time");
      reason = text(reason, "warranty decision reason");
      actor = text(actor, "actor");
    }
  }

  public record Diagnosis(UUID id, String findings, String recommendation, Instant diagnosedAt) {
    public Diagnosis {
      WarrantyPolicy.required(id, "diagnosis id");
      WarrantyPolicy.required(diagnosedAt, "diagnosis time");
      findings = text(findings, "diagnosis findings");
      recommendation = text(recommendation, "diagnosis recommendation");
    }
  }

  public record Resolution(UUID id, Type type, String outcome, Instant resolvedAt) {
    public Resolution {
      WarrantyPolicy.required(id, "resolution id");
      WarrantyPolicy.required(type, "resolution type");
      WarrantyPolicy.required(resolvedAt, "resolution time");
      outcome = text(outcome, "resolution outcome");
    }

    public enum Type {
      REPAIR,
      REPLACEMENT,
      CANCELLED
    }
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
}
