package com.newland.erp.enterprise.api;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public final class EnterpriseStructureOpenApiConfiguration {
    static final String GROUP = "enterprise-structure-v1";
    static final String PATH_PATTERN = "/api/v1/enterprise-structure/**";

    @Bean
    public GroupedOpenApi enterpriseStructureOpenApi() {
        return GroupedOpenApi.builder()
                .group(GROUP)
                .pathsToMatch(PATH_PATTERN)
                .build();
    }
}
