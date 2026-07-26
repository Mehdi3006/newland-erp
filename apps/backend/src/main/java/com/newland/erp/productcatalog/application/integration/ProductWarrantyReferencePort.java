package com.newland.erp.productcatalog.application.integration;

import java.util.Map;
import java.util.UUID;

public interface ProductWarrantyReferencePort {
  void requireProduct(UUID productId);

  WarrantyProduct requireWarrantyProduct(UUID productId, UUID skuId);

  record WarrantyProduct(
      UUID productId, UUID skuId, String productCode, String skuCode,
      Map<String, String> warrantyMetadata) {}
}
