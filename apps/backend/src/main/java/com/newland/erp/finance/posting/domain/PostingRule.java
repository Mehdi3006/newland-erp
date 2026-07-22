package com.newland.erp.finance.posting.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PostingRule(
    UUID postingRuleId,
    String code,
    String name,
    String eventType,
    UUID companyId,
    LocalDate effectiveFrom,
    LocalDate effectiveTo,
    int priority,
    Status status,
    int version,
    List<PostingRuleLine> lines,
    Instant createdAt,
    String createdBy,
    Instant updatedAt,
    String updatedBy) {
  public PostingRule {
    if (postingRuleId == null
        || code == null
        || code.isBlank()
        || eventType == null
        || eventType.isBlank()
        || effectiveFrom == null
        || (effectiveTo != null && effectiveTo.isBefore(effectiveFrom))
        || priority < 0
        || status == null
        || version < 1
        || lines == null
        || lines.size() < 2
        || createdAt == null
        || createdBy == null
        || createdBy.isBlank()) {
      throw new IllegalArgumentException("Posting rule metadata and effective dates are invalid.");
    }
    lines = List.copyOf(lines);
  }

  public boolean appliesTo(final AccountingEvent event) {
    return status == Status.ACTIVE
        && eventType.equals(event.eventType())
        && (companyId == null || companyId.equals(event.companyId()))
        && !event.accountingDate().isBefore(effectiveFrom)
        && (effectiveTo == null || !event.accountingDate().isAfter(effectiveTo));
  }

  public enum Status {
    DRAFT,
    ACTIVE,
    RETIRED
  }
}
