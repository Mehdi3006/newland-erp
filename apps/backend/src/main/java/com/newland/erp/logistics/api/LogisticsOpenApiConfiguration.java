package com.newland.erp.logistics.api;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LogisticsOpenApiConfiguration {
  @Bean
  GroupedOpenApi logisticsOpenApi() {
    return GroupedOpenApi.builder()
        .group("import-logistics-v1")
        .pathsToMatch("/api/v1/import-logistics/**")
        .build();
  }
}
