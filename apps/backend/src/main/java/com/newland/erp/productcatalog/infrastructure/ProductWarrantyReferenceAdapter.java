package com.newland.erp.productcatalog.infrastructure;

import com.newland.erp.productcatalog.application.ProductCatalogRepository;
import com.newland.erp.productcatalog.application.integration.ProductWarrantyReferencePort;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public final class ProductWarrantyReferenceAdapter implements ProductWarrantyReferencePort {
  private final ProductCatalogRepository repository;

  public ProductWarrantyReferenceAdapter(final ProductCatalogRepository catalogRepository) {
    repository = catalogRepository;
  }

  @Override
  public void requireProduct(final UUID productId) {
    repository
        .findById(productId)
        .orElseThrow(() -> new IllegalArgumentException("Warranty product not found."));
  }

  @Override
  public WarrantyProduct requireWarrantyProduct(final UUID productId, final UUID skuId) {
    final var product =
        repository
            .findById(productId)
            .orElseThrow(() -> new IllegalArgumentException("Warranty product not found."));
    final var sku =
        product.skus().stream()
            .filter(candidate -> candidate.id().equals(skuId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Warranty SKU not found in product."));
    return new WarrantyProduct(
        product.id(), sku.id(), product.productCode(), sku.skuCode(), product.warrantyMetadata());
  }
}
