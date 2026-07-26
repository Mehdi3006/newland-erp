package com.newland.erp.crm.api;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CrmOpenApiConfiguration {
  @Bean
  GroupedOpenApi crmOpenApi() {
    return GroupedOpenApi.builder()
        .group("crm-v1")
        .pathsToMatch("/api/v1/crm/**")
        .build();
  }
}
