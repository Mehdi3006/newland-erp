package com.newland.erp.procurement.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public final class ProcurementAccountingEventTest {
  @Test
  void preservesSupplierReferenceAndFinancialDimensionsWithoutAccountingRules() {
    final UUID supplierId = UUID.randomUUID();
    final UUID costCenterId = UUID.randomUUID();
    final ProcurementAccountingEvent event =
        event(
            ProcurementAccountingEvent.EventType.SUPPLIER_INVOICE_POSTED,
            supplierId,
            costCenterId);

    assertThat(event.postingAttributes())
        .containsEntry("supplierId", supplierId.toString())
        .containsEntry("referenceDocumentNumber", "INV-100");
    assertThat(event.postingDimensions())
        .containsEntry("costCenterId", costCenterId.toString())
        .containsEntry("project", "P-100");
  }

  @Test
  void rejectsNegativeAmountsAndUnsupportedStateCannotExist() {
    final ProcurementAccountingEvent valid =
        event(
            ProcurementAccountingEvent.EventType.GOODS_RECEIVED,
            UUID.randomUUID(),
            null);

    assertThatThrownBy(
            () ->
                new ProcurementAccountingEvent(
                    valid.eventId(),
                    valid.idempotencyKey(),
                    valid.eventType(),
                    valid.referenceDocumentType(),
                    valid.referenceDocumentId(),
                    valid.referenceDocumentNumber(),
                    valid.supplierId(),
                    valid.companyId(),
                    valid.branchId(),
                    valid.eventDate(),
                    valid.accountingDate(),
                    valid.currencyCode(),
                    valid.exchangeRate(),
                    new BigDecimal("-1"),
                    valid.taxAmount(),
                    valid.netAmount(),
                    null,
                    null,
                    Map.of(),
                    valid.description(),
                    valid.occurredAt(),
                    valid.actor()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  public static ProcurementAccountingEvent event(
      final ProcurementAccountingEvent.EventType type,
      final UUID supplierId,
      final UUID costCenterId) {
    return new ProcurementAccountingEvent(
        UUID.randomUUID(),
        "procurement-event-" + UUID.randomUUID(),
        type,
        "SUPPLIER_INVOICE",
        UUID.randomUUID(),
        "INV-100",
        supplierId,
        UUID.randomUUID(),
        UUID.randomUUID(),
        LocalDate.parse("2026-07-26"),
        LocalDate.parse("2026-07-26"),
        "USD",
        BigDecimal.ONE,
        new BigDecimal("100.00"),
        new BigDecimal("10.00"),
        new BigDecimal("90.00"),
        costCenterId,
        null,
        Map.of("project", "P-100"),
        "Supplier invoice",
        Instant.parse("2026-07-26T00:00:00Z"),
        UUID.randomUUID().toString());
  }
}
