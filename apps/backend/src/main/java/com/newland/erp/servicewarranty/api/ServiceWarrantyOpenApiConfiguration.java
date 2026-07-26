package com.newland.erp.servicewarranty.api;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServiceWarrantyOpenApiConfiguration {
  @Bean
  GroupedOpenApi serviceWarrantyOpenApi() {
    return GroupedOpenApi.builder()
        .group("service-warranty-v1")
        .pathsToMatch("/api/v1/service-warranty/**")
        .build();
  }
}
