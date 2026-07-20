package com.newland.erp.identity.api;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IdentityOpenApiConfiguration {
    static final String GROUP = "identity-access-v1";
    static final String[] PATHS = {"/api/v1/auth/**", "/api/v1/identity/**", "/api/v1/access-control/**"};

    @Bean
    GroupedOpenApi identityAccessOpenApi() {
        return GroupedOpenApi.builder().group(GROUP).pathsToMatch(PATHS).build();
    }
}
