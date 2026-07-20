package com.newland.erp.platform.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

final class PlatformOpenApiConfigurationTest {
    @Test
    void exposesPlatformOpenApiGroup() {
        final var openApi = new PlatformOpenApiConfiguration().platformOpenApi();

        assertThat(openApi.getGroup()).isEqualTo("platform-foundation-v1");
        assertThat(openApi.getPathsToMatch()).containsExactly("/api/v1/platform/**");
    }
}
