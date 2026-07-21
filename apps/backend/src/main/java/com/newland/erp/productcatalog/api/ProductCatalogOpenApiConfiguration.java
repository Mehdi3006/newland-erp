package com.newland.erp.productcatalog.api;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProductCatalogOpenApiConfiguration {
    static final String GROUP = "product-catalog-v1";

    @Bean
    GroupedOpenApi productCatalogOpenApi() {
        return GroupedOpenApi.builder().group(GROUP).pathsToMatch("/api/v1/product-catalog/**").build();
    }
}
