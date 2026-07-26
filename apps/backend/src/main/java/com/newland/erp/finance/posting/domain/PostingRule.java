package com.newland.erp.finance.posting.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
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
        || name == null
        || name.isBlank()
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
    if (lines.stream().map(PostingRuleLine::lineNumber).distinct().count() != lines.size()) {
      throw new PostingException("Posting rule line numbers must be unique.");
    }
    if (lines.stream().noneMatch(line -> line.direction() == PostingRuleLine.Direction.DEBIT)
        || lines.stream().noneMatch(line -> line.direction() == PostingRuleLine.Direction.CREDIT)) {
      throw new PostingException("Posting rules require debit and credit lines.");
    }
    if (companyId == null
        && lines.stream()
            .anyMatch(
                line ->
                    line.accountResolutionType()
                        == PostingRuleLine.AccountResolutionType.FIXED_ACCOUNT)) {
      throw new PostingException(
          "Global posting rules must resolve accounts from event attributes.");
    }
  }

  public PostingRule activate(final Instant changedAt, final String actor) {
    requireTransition(Status.DRAFT, "Only a draft posting rule can be activated.");
    return withStatus(Status.ACTIVE, changedAt, actor);
  }

  public PostingRule retire(final Instant changedAt, final String actor) {
    requireTransition(Status.ACTIVE, "Only an active posting rule can be retired.");
    return withStatus(Status.RETIRED, changedAt, actor);
  }

  public PostingRule successor(
      final UUID successorId,
      final String successorName,
      final LocalDate successorEffectiveFrom,
      final LocalDate successorEffectiveTo,
      final int successorPriority,
      final List<PostingRuleLine> successorLines,
      final Instant changedAt,
      final String actor) {
    if (status == Status.DRAFT) {
      throw new PostingException("A draft posting rule cannot have a successor version.");
    }
    return new PostingRule(
        successorId,
        code,
        successorName,
        eventType,
        companyId,
        successorEffectiveFrom,
        successorEffectiveTo,
        successorPriority,
        Status.DRAFT,
        version + 1,
        successorLines,
        changedAt,
        actor,
        null,
        null);
  }

  public boolean conflictsWith(final PostingRule candidate) {
    if (!eventType.equals(candidate.eventType)
        || !Objects.equals(companyId, candidate.companyId)
        || !(code.equals(candidate.code) || priority == candidate.priority)) {
      return false;
    }
    return !endsBefore(candidate) && !candidate.endsBefore(this);
  }

  public boolean appliesTo(final AccountingEvent event) {
    return status == Status.ACTIVE
        && eventType.equals(event.eventType())
        && (companyId == null || companyId.equals(event.companyId()))
        && !event.accountingDate().isBefore(effectiveFrom)
        && (effectiveTo == null || !event.accountingDate().isAfter(effectiveTo));
  }

  private boolean endsBefore(final PostingRule other) {
    return effectiveTo != null && effectiveTo.isBefore(other.effectiveFrom);
  }

  private void requireTransition(final Status required, final String message) {
    if (status != required) {
      throw new PostingException(message);
    }
  }

  private PostingRule withStatus(
      final Status newStatus, final Instant changedAt, final String actor) {
    if (changedAt == null || actor == null || actor.isBlank()) {
      throw new PostingException("Posting rule lifecycle audit data is required.");
    }
    return new PostingRule(
        postingRuleId,
        code,
        name,
        eventType,
        companyId,
        effectiveFrom,
        effectiveTo,
        priority,
        newStatus,
        version,
        lines,
        createdAt,
        createdBy,
        changedAt,
        actor);
  }

  public enum Status {
    DRAFT,
    ACTIVE,
    RETIRED
  }
}
