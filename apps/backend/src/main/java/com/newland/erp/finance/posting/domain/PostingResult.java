package com.newland.erp.finance.posting.domain;

import java.util.UUID;

public record PostingResult(
    UUID postingRequestId,
    UUID eventId,
    PostingRequest.Status status,
    UUID journalEntryId,
    String journalNumber,
    String failureCode,
    String failureMessage) {}
