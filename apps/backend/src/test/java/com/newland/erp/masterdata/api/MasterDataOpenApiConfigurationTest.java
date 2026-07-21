package com.newland.erp.masterdata.api;

import org.junit.jupiter.api.Test;
import org.springdoc.core.models.GroupedOpenApi;

import static org.assertj.core.api.Assertions.assertThat;

final class MasterDataOpenApiConfigurationTest {
    @Test
    void exposesMasterDataOpenApiGroup() {
        final GroupedOpenApi api = new MasterDataOpenApiConfiguration().masterDataOpenApi();

        assertThat(api.getGroup()).isEqualTo("master-data-v1");
        assertThat(api.getPathsToMatch()).containsExactly("/api/v1/master-data/**");
    }
}
