package com.newland.erp.sales.infrastructure;

import com.newland.erp.sales.application.SalesRepository;
import com.newland.erp.sales.application.integration.SalesWarrantyEvidencePort;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public final class SalesWarrantyEvidenceAdapter implements SalesWarrantyEvidencePort {
  private final SalesRepository repository;

  public SalesWarrantyEvidenceAdapter(final SalesRepository salesRepository) {
    repository = salesRepository;
  }

  @Override
  public SalesEvidence requireDeliveredEvidence(
      final UUID salesOrderId,
      final UUID companyId,
      final UUID customerId,
      final UUID productId,
      final UUID skuId) {
    final var order =
        repository
            .findSalesOrder(salesOrderId)
            .orElseThrow(() -> new IllegalArgumentException("Sales warranty evidence not found."));
    final boolean delivered =
        order.companyId().equals(companyId)
            && order.customerId().equals(customerId)
            && order.lines().stream()
                .anyMatch(
                    line ->
                        line.productId().equals(productId)
                            && line.skuId().equals(skuId)
                            && line.deliveredQuantity().isPositive());
    if (!delivered) {
      throw new IllegalArgumentException("Sales order does not prove delivered warranty product.");
    }
    return new SalesEvidence(
        order.id(), order.createdAt().atZone(ZoneOffset.UTC).toLocalDate());
  }
}
