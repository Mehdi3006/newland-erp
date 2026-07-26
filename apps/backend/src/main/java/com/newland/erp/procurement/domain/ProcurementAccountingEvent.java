package com.newland.erp.procurement.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Immutable Procurement-owned business fact submitted to Finance for rule-based posting. */
public record ProcurementAccountingEvent(
    UUID eventId,
    String idempotencyKey,
    EventType eventType,
    String referenceDocumentType,
    UUID referenceDocumentId,
    String referenceDocumentNumber,
    UUID supplierId,
    UUID companyId,
    UUID branchId,
    LocalDate eventDate,
    LocalDate accountingDate,
    String currencyCode,
    BigDecimal exchangeRate,
    BigDecimal amount,
    BigDecimal taxAmount,
    BigDecimal netAmount,
    UUID costCenterId,
    UUID profitCenterId,
    Map<String, String> financialDimensions,
    String description,
    Instant occurredAt,
    String actor) {
  public ProcurementAccountingEvent {
    require(eventId, "event id");
    require(referenceDocumentId, "reference document id");
    require(supplierId, "supplier id");
    require(companyId, "company id");
    require(branchId, "branch id");
    require(eventType, "event type");
    idempotencyKey = requiredText(idempotencyKey, "idempotency key");
    referenceDocumentType = requiredText(referenceDocumentType, "reference document type");
    referenceDocumentNumber = requiredText(referenceDocumentNumber, "reference document number");
    currencyCode = requiredText(currencyCode, "currency code").toUpperCase();
    description = requiredText(description, "description");
    actor = requiredText(actor, "actor");
    require(eventDate, "event date");
    require(accountingDate, "accounting date");
    require(occurredAt, "occurred at");
    if (exchangeRate == null || exchangeRate.signum() <= 0) {
      throw new IllegalArgumentException("Procurement exchange rate must be positive.");
    }
    if (amount == null || amount.signum() < 0
        || taxAmount == null || taxAmount.signum() < 0
        || netAmount == null || netAmount.signum() < 0) {
      throw new IllegalArgumentException("Procurement accounting amounts cannot be negative.");
    }
    financialDimensions =
        financialDimensions == null ? Map.of() : Map.copyOf(financialDimensions);
  }

  public Map<String, String> postingDimensions() {
    final Map<String, String> values = new LinkedHashMap<>(financialDimensions);
    if (costCenterId != null) {
      values.put("costCenterId", costCenterId.toString());
    }
    if (profitCenterId != null) {
      values.put("profitCenterId", profitCenterId.toString());
    }
    return Map.copyOf(values);
  }

  public Map<String, String> postingAttributes() {
    return Map.of(
        "supplierId", supplierId.toString(),
        "referenceDocumentType", referenceDocumentType,
        "referenceDocumentNumber", referenceDocumentNumber);
  }

  public enum EventType {
    PURCHASE_ORDER_APPROVED("PurchaseOrderApproved"),
    GOODS_RECEIVED("GoodsReceived"),
    SUPPLIER_INVOICE_POSTED("SupplierInvoicePosted"),
    SUPPLIER_CREDIT_NOTE_POSTED("SupplierCreditNotePosted"),
    SUPPLIER_PAYMENT_POSTED("SupplierPaymentPosted");

    private final String financeEventType;

    EventType(final String value) {
      financeEventType = value;
    }

    public String financeEventType() {
      return financeEventType;
    }
  }

  private static void require(final Object value, final String name) {
    if (value == null) {
      throw new IllegalArgumentException("Procurement " + name + " is required.");
    }
  }

  private static String requiredText(final String value, final String name) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Procurement " + name + " is required.");
    }
    return value.trim();
  }
}
