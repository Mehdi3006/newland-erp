package com.newland.erp.identity.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

final class IdentityOpenApiConfigurationTest {
    @Test
    void exposesIdentityAccessOpenApiGroup() {
        final var openApi = new IdentityOpenApiConfiguration().identityAccessOpenApi();

        assertThat(openApi.getGroup()).isEqualTo("identity-access-v1");
        assertThat(openApi.getPathsToMatch()).containsExactly(
                "/api/v1/auth/**",
                "/api/v1/identity/**",
                "/api/v1/access-control/**"
        );
    }
}
