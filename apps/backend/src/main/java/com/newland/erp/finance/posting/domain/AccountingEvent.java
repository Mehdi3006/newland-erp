package com.newland.erp.finance.posting.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;
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

  /**
   * Compares the durable idempotency payload while excluding the ingestion timestamp. A retried
   * HTTP request receives a new {@code occurredAt}, but every caller-controlled business field
   * must still match the accepted event.
   */
  public boolean hasSameIdempotencyPayload(final AccountingEvent candidate) {
    return candidate != null
        && Objects.equals(eventId, candidate.eventId)
        && Objects.equals(idempotencyKey, candidate.idempotencyKey)
        && Objects.equals(eventType, candidate.eventType)
        && Objects.equals(sourceModule, candidate.sourceModule)
        && Objects.equals(sourceDocumentType, candidate.sourceDocumentType)
        && Objects.equals(sourceDocumentId, candidate.sourceDocumentId)
        && Objects.equals(sourceDocumentNumber, candidate.sourceDocumentNumber)
        && Objects.equals(companyId, candidate.companyId)
        && Objects.equals(branchId, candidate.branchId)
        && Objects.equals(eventDate, candidate.eventDate)
        && Objects.equals(accountingDate, candidate.accountingDate)
        && Objects.equals(currencyCode, candidate.currencyCode)
        && sameAmount(exchangeRate, candidate.exchangeRate)
        && sameAmount(amount, candidate.amount)
        && sameAmount(taxAmount, candidate.taxAmount)
        && sameAmount(netAmount, candidate.netAmount)
        && Objects.equals(description, candidate.description)
        && Objects.equals(dimensions, candidate.dimensions)
        && Objects.equals(attributes, candidate.attributes)
        && Objects.equals(submittedBy, candidate.submittedBy)
        && version == candidate.version;
  }

  private static boolean sameAmount(final BigDecimal first, final BigDecimal second) {
    return first == null ? second == null : second != null && first.compareTo(second) == 0;
  }
}
