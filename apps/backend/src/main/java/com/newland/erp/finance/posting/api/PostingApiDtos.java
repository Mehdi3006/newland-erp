package com.newland.erp.finance.posting.api;

import com.newland.erp.finance.posting.domain.PostingRequest;
import com.newland.erp.finance.posting.domain.PostingResult;
import java.time.Instant;
import java.util.UUID;

public final class PostingApiDtos {
  public record PostingResultResponse(
      UUID postingRequestId,
      UUID eventId,
      String status,
      UUID journalEntryId,
      String journalNumber,
      String failureCode,
      String failureMessage) {
    static PostingResultResponse from(final PostingResult result) {
      return new PostingResultResponse(
          result.postingRequestId(),
          result.eventId(),
          result.status().name(),
          result.journalEntryId(),
          result.journalNumber(),
          result.failureCode(),
          result.failureMessage());
    }
  }

  public record PostingRequestResponse(
      UUID postingRequestId,
      UUID accountingEventId,
      String status,
      UUID resolvedPostingRuleId,
      Integer resolvedPostingRuleVersion,
      UUID journalEntryId,
      String failureCode,
      String failureMessage,
      int attempts,
      Instant createdAt,
      Instant updatedAt,
      int version) {
    static PostingRequestResponse from(final PostingRequest request) {
      return new PostingRequestResponse(
          request.postingRequestId(),
          request.accountingEventId(),
          request.status().name(),
          request.resolvedPostingRuleId(),
          request.resolvedPostingRuleVersion(),
          request.journalEntryId(),
          request.failureCode(),
          request.failureMessage(),
          request.attempts(),
          request.createdAt(),
          request.updatedAt(),
          request.version());
    }
  }

  private PostingApiDtos() {}
}
