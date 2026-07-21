package com.newland.erp.masterdata.api;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MasterDataOpenApiConfiguration {
    static final String GROUP = "master-data-v1";

    @Bean
    GroupedOpenApi masterDataOpenApi() {
        return GroupedOpenApi.builder().group(GROUP).pathsToMatch("/api/v1/master-data/**").build();
    }
}
