package com.newland.erp.productcatalog.application;

import com.newland.erp.productcatalog.domain.Product;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductCatalogRepository {
    Product insert(Product product);

    Product update(Product product);

    Optional<Product> findById(UUID id);

    Optional<Product> findByProductCode(String productCode);

    boolean skuCodeExists(String skuCode);

    boolean tradeIdentifierExists(String identifier);

    List<Product> listProducts();
}
