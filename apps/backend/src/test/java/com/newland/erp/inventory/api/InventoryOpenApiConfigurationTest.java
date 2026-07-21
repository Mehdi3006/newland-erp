package com.newland.erp.inventory.api;

import org.junit.jupiter.api.Test;
import org.springdoc.core.models.GroupedOpenApi;

import static org.assertj.core.api.Assertions.assertThat;

final class InventoryOpenApiConfigurationTest {
    @Test
    void exposesInventoryApiMetadata() {
        final GroupedOpenApi openApi = new InventoryOpenApiConfiguration().inventoryOpenApi();

        assertThat(openApi.getGroup()).isEqualTo("inventory-v1");
    }
}
