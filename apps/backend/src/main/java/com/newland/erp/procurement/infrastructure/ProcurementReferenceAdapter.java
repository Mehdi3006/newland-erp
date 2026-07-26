package com.newland.erp.procurement.infrastructure;

import com.newland.erp.procurement.application.ProcurementRepository;
import com.newland.erp.procurement.application.integration.ProcurementReferencePort;
import com.newland.erp.procurement.domain.ProcurementNotFoundException;
import com.newland.erp.procurement.domain.PurchaseOrderStatus;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public final class ProcurementReferenceAdapter implements ProcurementReferencePort {
  private final ProcurementRepository repository;

  public ProcurementReferenceAdapter(final ProcurementRepository procurementRepository) {
    repository = procurementRepository;
  }

  @Override
  public PurchaseOrderReference requireApprovedPurchaseOrder(final UUID purchaseOrderId) {
    final var order =
        repository
            .findPurchaseOrder(purchaseOrderId)
            .orElseThrow(() -> new ProcurementNotFoundException("Purchase order not found."));
    if (order.status() != PurchaseOrderStatus.APPROVED
        && order.status() != PurchaseOrderStatus.PARTIALLY_RECEIVED) {
      throw new IllegalStateException("Import shipment requires an approved purchase order.");
    }
    return new PurchaseOrderReference(
        order.id(),
        order.orderNumber(),
        order.supplierId(),
        order.companyId(),
        order.branchId(),
        order.warehouseId(),
        order.currencyId());
  }
}
