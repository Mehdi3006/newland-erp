package com.newland.erp.enterprise.api;

import org.junit.jupiter.api.Test;
import org.springdoc.core.models.GroupedOpenApi;

import static org.assertj.core.api.Assertions.assertThat;

final class EnterpriseStructureOpenApiConfigurationTest {
    @Test
    void exposesVersionedEnterpriseStructureOpenApiGroup() {
        final GroupedOpenApi openApi =
                new EnterpriseStructureOpenApiConfiguration().enterpriseStructureOpenApi();

        assertThat(openApi.getGroup()).isEqualTo("enterprise-structure-v1");
        assertThat(openApi.getPathsToMatch()).containsExactly("/api/v1/enterprise-structure/**");
    }
}
