package com.newland.erp.productcatalog.application;

import com.newland.erp.productcatalog.domain.Product;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

final class InMemoryProductCatalogRepository implements ProductCatalogRepository {
    private final List<Product> products = new ArrayList<>();

    @Override
    public Product insert(final Product product) {
        products.add(product);
        return product;
    }

    @Override
    public Product update(final Product product) {
        products.removeIf(existing -> existing.id().equals(product.id()));
        products.add(product);
        return product;
    }

    @Override
    public Optional<Product> findById(final UUID id) {
        return products.stream().filter(product -> product.id().equals(id)).findFirst();
    }

    @Override
    public Optional<Product> findByProductCode(final String productCode) {
        return products.stream().filter(product -> product.productCode().equals(productCode.toUpperCase()))
                .findFirst();
    }

    @Override
    public boolean skuCodeExists(final String skuCode) {
        return products.stream().flatMap(product -> product.skus().stream())
                .anyMatch(sku -> sku.skuCode().equals(skuCode.toUpperCase()));
    }

    @Override
    public boolean tradeIdentifierExists(final String identifier) {
        return products.stream().flatMap(product -> product.skus().stream())
                .anyMatch(sku -> identifier.equals(sku.gtin()) || identifier.equals(sku.ean())
                        || identifier.equals(sku.upc()) || identifier.equals(sku.barcode()));
    }

    @Override
    public List<Product> listProducts() {
        return List.copyOf(products);
    }
}
