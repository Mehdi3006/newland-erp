package com.newland.erp.productcatalog.api;

import org.junit.jupiter.api.Test;
import org.springdoc.core.models.GroupedOpenApi;

import static org.assertj.core.api.Assertions.assertThat;

final class ProductCatalogOpenApiConfigurationTest {
    @Test
    void exposesProductCatalogOpenApiGroup() {
        final GroupedOpenApi api = new ProductCatalogOpenApiConfiguration().productCatalogOpenApi();

        assertThat(api.getGroup()).isEqualTo("product-catalog-v1");
        assertThat(api.getPathsToMatch()).containsExactly("/api/v1/product-catalog/**");
    }
}
