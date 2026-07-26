package com.newland.erp.sales.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.newland.erp.sales.application.SalesRepository;
import com.newland.erp.sales.domain.SalesOrder;
import com.newland.erp.sales.domain.SalesOrderStatus;
import com.newland.erp.sales.domain.SalesQuantity;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class SalesWarrantyEvidenceAdapterTest {
  @Test
  void usesAuthoritativeLineDeliveryDateRatherThanOrderCreationDate() {
    final SalesRepository repository = mock(SalesRepository.class);
    final UUID orderId = UUID.randomUUID();
    final UUID companyId = UUID.randomUUID();
    final UUID customerId = UUID.randomUUID();
    final UUID productId = UUID.randomUUID();
    final UUID skuId = UUID.randomUUID();
    final Instant orderCreatedAt = Instant.parse("2025-12-01T00:00:00Z");
    final Instant deliveredAt = Instant.parse("2026-02-15T10:30:00Z");
    final SalesQuantity zero = quantity("0");
    final SalesOrder.SalesOrderLine line =
        new SalesOrder.SalesOrderLine(
            UUID.randomUUID(),
            productId,
            skuId,
            "SKU-1",
            quantity("1"),
            zero,
            quantity("1"),
            zero,
            UUID.randomUUID(),
            deliveredAt);
    final SalesOrder order =
        new SalesOrder(
            orderId,
            "SO-1",
            "order-key",
            null,
            customerId,
            companyId,
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            SalesOrderStatus.DELIVERED,
            0,
            List.of(line),
            0,
            LocalDate.of(2026, 2, 1),
            orderCreatedAt,
            "actor");
    when(repository.findSalesOrder(orderId)).thenReturn(Optional.of(order));

    final var evidence =
        new SalesWarrantyEvidenceAdapter(repository)
            .requireDeliveredEvidence(orderId, companyId, customerId, productId, skuId);

    assertThat(evidence.soldOn()).isEqualTo(LocalDate.of(2026, 2, 15));
    assertThat(evidence.soldOn()).isNotEqualTo(LocalDate.of(2025, 12, 1));
  }

  private static SalesQuantity quantity(final String value) {
    return new SalesQuantity(new BigDecimal(value), "EA");
  }
}
