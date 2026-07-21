package com.newland.erp.procurement.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

final class ProcurementOpenApiConfigurationTest {
    @Test
    void exposesProcurementApiGroup() {
        assertThat(new ProcurementOpenApiConfiguration().procurementOpenApi().getGroup())
                .isEqualTo("procurement-v1");
    }
}
