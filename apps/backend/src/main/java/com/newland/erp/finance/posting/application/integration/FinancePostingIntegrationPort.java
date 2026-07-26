package com.newland.erp.finance.posting.application.integration;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Locale;
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
      String submittedBy) {
    public AccountingEventMessage {
      require(eventId, "event id");
      idempotencyKey = text(idempotencyKey, "idempotency key");
      eventType = text(eventType, "event type");
      sourceModule = text(sourceModule, "source module");
      sourceDocumentType = text(sourceDocumentType, "source document type");
      require(sourceDocumentId, "source document id");
      sourceDocumentNumber = text(sourceDocumentNumber, "source document number");
      require(companyId, "company id");
      require(branchId, "branch id");
      require(eventDate, "event date");
      require(accountingDate, "accounting date");
      currencyCode = text(currencyCode, "currency code").toUpperCase(Locale.ROOT);
      if (!currencyCode.matches("[A-Z]{3}")) {
        throw new IllegalArgumentException("Currency must be a three-letter code.");
      }
      if (exchangeRate == null || exchangeRate.signum() <= 0) {
        throw new IllegalArgumentException("Exchange rate must be positive.");
      }
      if (amount == null || amount.signum() < 0) {
        throw new IllegalArgumentException("Posting amount cannot be missing or negative.");
      }
      if (taxAmount != null && taxAmount.signum() < 0) {
        throw new IllegalArgumentException("Tax amount cannot be negative.");
      }
      if (netAmount != null && netAmount.signum() < 0) {
        throw new IllegalArgumentException("Net amount cannot be negative.");
      }
      description = text(description, "description");
      dimensions = dimensions == null ? Map.of() : Map.copyOf(dimensions);
      attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
      require(occurredAt, "occurred at");
      submittedBy = text(submittedBy, "submitted by");
    }

    private static String text(final String value, final String name) {
      if (value == null || value.isBlank()) {
        throw new IllegalArgumentException(name + " is required.");
      }
      return value.trim();
    }

    private static void require(final Object value, final String name) {
      if (value == null) {
        throw new IllegalArgumentException(name + " is required.");
      }
    }
  }

  record PostingReceipt(
      UUID postingRequestId,
      UUID accountingEventId,
      String status,
      UUID journalEntryId,
      String journalNumber,
      String failureCode,
      String failureMessage) {}
}
