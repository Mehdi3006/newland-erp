package com.newland.erp.sales.application.integration;

import java.time.LocalDate;
import java.util.UUID;

public interface SalesWarrantyEvidencePort {
  SalesEvidence requireDeliveredEvidence(
      UUID salesOrderId, UUID companyId, UUID customerId, UUID productId, UUID skuId);

  record SalesEvidence(UUID salesOrderId, LocalDate soldOn) {}
}
