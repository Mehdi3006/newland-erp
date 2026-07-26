package com.newland.erp.finance.posting.application.integration;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/** Published Finance API for bounded contexts that submit immutable accounting facts. */
public interface FinancePostingIntegrationPort {
  PostingReceipt publish(AccountingEventMessage message);

  PostingReceipt retry(UUID postingRequestId);

  record AccountingEventMessage(
      UUID eventId,
      String idempotencyKey,
      String eventType,
      String sourceModule,
      String sourceDocumentType,
      UUID sourceDocumentId,
      String sourceDocumentNumber,
      UUID companyId,
      UUID branchId,
      LocalDate eventDate,
      LocalDate accountingDate,
      String currencyCode,
      BigDecimal exchangeRate,
      BigDecimal amount,
      BigDecimal taxAmount,
      BigDecimal netAmount,
      String description,
      Map<String, String> dimensions,
      Map<String, String> attributes,
      Instant occurredAt,
      String submittedBy) {}

  record PostingReceipt(
      UUID postingRequestId,
      UUID accountingEventId,
      String status,
      UUID journalEntryId,
      String journalNumber,
      String failureCode,
      String failureMessage) {}
}
