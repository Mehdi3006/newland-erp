package com.newland.erp.procurement.application.integration;

import java.util.UUID;

/** Published Procurement reference API for downstream bounded contexts. */
public interface ProcurementReferencePort {
  PurchaseOrderReference requireApprovedPurchaseOrder(UUID purchaseOrderId);

  record PurchaseOrderReference(
      UUID purchaseOrderId,
      String orderNumber,
      UUID supplierId,
      UUID companyId,
      UUID branchId,
      UUID warehouseId,
      UUID currencyId) {}
}
