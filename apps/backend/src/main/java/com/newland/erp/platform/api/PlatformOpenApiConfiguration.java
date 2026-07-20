package com.newland.erp.platform.api;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PlatformOpenApiConfiguration {
    static final String GROUP = "platform-foundation-v1";

    @Bean
    GroupedOpenApi platformOpenApi() {
        return GroupedOpenApi.builder().group(GROUP).pathsToMatch("/api/v1/platform/**").build();
    }
}
