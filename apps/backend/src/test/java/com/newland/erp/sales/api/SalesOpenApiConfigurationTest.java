package com.newland.erp.sales.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

final class SalesOpenApiConfigurationTest {
    @Test
    void exposesSalesApiGroup() {
        assertThat(new SalesOpenApiConfiguration().salesOpenApi().getGroup()).isEqualTo("sales-v1");
    }
}
