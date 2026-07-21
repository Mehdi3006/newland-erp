package com.newland.erp.sales.api;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SalesOpenApiConfiguration {
    static final String GROUP = "sales-v1";

    @Bean
    GroupedOpenApi salesOpenApi() {
        return GroupedOpenApi.builder().group(GROUP).pathsToMatch("/api/v1/sales/**").build();
    }
}
