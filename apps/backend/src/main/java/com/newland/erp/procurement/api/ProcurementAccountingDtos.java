package com.newland.erp.procurement.api;

import com.newland.erp.procurement.application.ProcurementAccountingService;
import com.newland.erp.procurement.domain.ProcurementAccountingEvent;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public final class ProcurementAccountingDtos {
  public record PublishRequest(
      @NotNull UUID eventId,
      @NotBlank String idempotencyKey,
      @NotBlank String eventType,
      @NotBlank String referenceDocumentType,
      @NotNull UUID referenceDocumentId,
      @NotBlank String referenceDocumentNumber,
      @NotNull UUID supplierId,
      @NotNull UUID companyId,
      @NotNull UUID branchId,
      @NotNull LocalDate eventDate,
      @NotNull LocalDate accountingDate,
      @NotBlank String currencyCode,
      @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal exchangeRate,
      @NotNull @DecimalMin("0.0") BigDecimal amount,
      @NotNull @DecimalMin("0.0") BigDecimal taxAmount,
      @NotNull @DecimalMin("0.0") BigDecimal netAmount,
      UUID costCenterId,
      UUID profitCenterId,
      Map<String, String> financialDimensions,
      @NotBlank String description,
      @NotNull Instant occurredAt) {
    ProcurementAccountingEvent toDomain(final String actor) {
      return new ProcurementAccountingEvent(
          eventId,
          idempotencyKey,
          ProcurementAccountingDtos.eventType(eventType),
          referenceDocumentType,
          referenceDocumentId,
          referenceDocumentNumber,
          supplierId,
          companyId,
          branchId,
          eventDate,
          accountingDate,
          currencyCode,
          exchangeRate,
          amount,
          taxAmount,
          netAmount,
          costCenterId,
          profitCenterId,
          financialDimensions,
          description,
          occurredAt,
          actor);
    }
  }

  public record RetryRequest(@NotNull UUID companyId) {}

  public record PostingResponse(
      UUID postingRequestId,
      UUID accountingEventId,
      String status,
      UUID journalEntryId,
      String journalNumber,
      String failureCode,
      String failureMessage) {
    static PostingResponse from(final ProcurementAccountingService.PostingReceipt receipt) {
      return new PostingResponse(
          receipt.postingRequestId(),
          receipt.accountingEventId(),
          receipt.status(),
          receipt.journalEntryId(),
          receipt.journalNumber(),
          receipt.failureCode(),
          receipt.failureMessage());
    }
  }

  private static ProcurementAccountingEvent.EventType eventType(final String value) {
    for (final ProcurementAccountingEvent.EventType candidate
        : ProcurementAccountingEvent.EventType.values()) {
      if (candidate.financeEventType().equals(value)) {
        return candidate;
      }
    }
    throw new IllegalArgumentException("Unsupported Procurement accounting event type.");
  }

  private ProcurementAccountingDtos() {}
}
