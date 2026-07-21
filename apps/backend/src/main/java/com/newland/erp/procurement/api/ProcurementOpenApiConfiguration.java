package com.newland.erp.procurement.api;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProcurementOpenApiConfiguration {
    static final String GROUP = "procurement-v1";

    @Bean
    GroupedOpenApi procurementOpenApi() {
        return GroupedOpenApi.builder().group(GROUP).pathsToMatch("/api/v1/procurement/**").build();
    }
}
