package com.newland.erp.inventory.api;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InventoryOpenApiConfiguration {
    static final String GROUP = "inventory-v1";

    @Bean
    GroupedOpenApi inventoryOpenApi() {
        return GroupedOpenApi.builder().group(GROUP).pathsToMatch("/api/v1/inventory/**").build();
    }
}
