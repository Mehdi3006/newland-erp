package com.newland.erp.finance.posting.domain;

import java.time.Instant;
import java.util.UUID;

public record PostingRequest(
    UUID postingRequestId,
    UUID accountingEventId,
    Status status,
    UUID resolvedPostingRuleId,
    Integer resolvedPostingRuleVersion,
    UUID journalEntryId,
    String failureCode,
    String failureMessage,
    int attempts,
    Instant createdAt,
    Instant updatedAt,
    int version) {
  public PostingRequest {
    if (postingRequestId == null
        || accountingEventId == null
        || status == null
        || attempts < 0
        || createdAt == null
        || updatedAt == null
        || version < 0) {
      throw new IllegalArgumentException("Posting request metadata is invalid.");
    }
  }

  public enum Status {
    RECEIVED,
    VALIDATING,
    RULE_RESOLVED,
    JOURNAL_CREATED,
    POSTED,
    FAILED,
    REJECTED
  }
}
