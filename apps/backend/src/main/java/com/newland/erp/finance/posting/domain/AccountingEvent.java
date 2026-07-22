package com.newland.erp.finance.posting.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public record AccountingEvent(
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
    String submittedBy,
    int version) {
  public AccountingEvent {
    if (eventId == null
        || idempotencyKey == null
        || idempotencyKey.isBlank()
        || eventType == null
        || eventType.isBlank()
        || sourceModule == null
        || sourceModule.isBlank()
        || sourceDocumentId == null
        || companyId == null
        || branchId == null
        || eventDate == null
        || accountingDate == null
        || currencyCode == null
        || currencyCode.isBlank()
        || exchangeRate == null
        || exchangeRate.signum() <= 0
        || amount == null
        || amount.signum() < 0
        || occurredAt == null
        || submittedBy == null
        || submittedBy.isBlank()) {
      throw new IllegalArgumentException(
          "Accounting event metadata and monetary values are required.");
    }
    dimensions = dimensions == null ? Map.of() : Map.copyOf(dimensions);
    attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
  }
}
