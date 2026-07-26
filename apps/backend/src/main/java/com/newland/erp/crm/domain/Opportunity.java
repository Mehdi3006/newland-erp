package com.newland.erp.crm.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

public record Opportunity(
    UUID id,
    String idempotencyKey,
    UUID companyId,
    UUID branchId,
    UUID ownerId,
    UUID leadId,
    UUID customerId,
    String opportunityNumber,
    String name,
    Stage stage,
    BigDecimal estimatedValue,
    String currencyCode,
    int probabilityPercent,
    LocalDate expectedCloseDate,
    String closureReason,
    long version,
    Instant createdAt,
    Instant updatedAt,
    String actor) {
  public Opportunity {
    Lead.require(id, "opportunity id");
    Lead.require(companyId, "company id");
    Lead.require(ownerId, "owner id");
    Lead.require(stage, "opportunity stage");
    Lead.require(expectedCloseDate, "expected close date");
    Lead.require(createdAt, "created at");
    Lead.require(updatedAt, "updated at");
    idempotencyKey = Lead.text(idempotencyKey, "idempotency key");
    opportunityNumber =
        Lead.text(opportunityNumber, "opportunity number").toUpperCase(Locale.ROOT);
    name = Lead.text(name, "opportunity name");
    currencyCode = Lead.text(currencyCode, "currency code").toUpperCase(Locale.ROOT);
    closureReason = Lead.optional(closureReason);
    actor = Lead.text(actor, "actor");
    if (leadId == null && customerId == null) {
      throw new IllegalArgumentException("Opportunity must reference a lead or customer.");
    }
    if (estimatedValue == null || estimatedValue.signum() < 0) {
      throw new IllegalArgumentException("Opportunity value cannot be negative.");
    }
    if (probabilityPercent < 0 || probabilityPercent > 100 || version < 0) {
      throw new IllegalArgumentException("Opportunity probability or version is invalid.");
    }
    if ((stage == Stage.WON || stage == Stage.LOST) && closureReason.isBlank()) {
      throw new IllegalArgumentException("Closed opportunity requires a closure reason.");
    }
  }

  public Opportunity advance(final Stage next, final String reason, final Instant now) {
    if (stage == Stage.WON || stage == Stage.LOST) {
      throw new IllegalStateException("Closed opportunities are immutable.");
    }
    if (!stage.canTransitionTo(next)) {
      throw new IllegalStateException("Invalid opportunity stage transition.");
    }
    final String resolvedReason =
        next == Stage.WON || next == Stage.LOST
            ? Lead.text(reason, "closure reason")
            : "";
    return new Opportunity(
        id, idempotencyKey, companyId, branchId, ownerId, leadId, customerId,
        opportunityNumber, name, next, estimatedValue, currencyCode,
        next.probability(), expectedCloseDate, resolvedReason, version + 1, createdAt,
        java.util.Objects.requireNonNull(now), actor);
  }

  public enum Stage {
    QUALIFICATION(20),
    DISCOVERY(40),
    PROPOSAL(60),
    NEGOTIATION(80),
    WON(100),
    LOST(0);

    private final int probability;

    Stage(final int defaultProbability) {
      probability = defaultProbability;
    }

    public int probability() {
      return probability;
    }

    boolean canTransitionTo(final Stage next) {
      if (next == null || next == this) {
        return false;
      }
      if (next == LOST) {
        return true;
      }
      return next.ordinal() == ordinal() + 1;
    }
  }
}
