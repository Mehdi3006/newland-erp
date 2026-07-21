package com.newland.erp.masterdata.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

final class MasterDataDomainTest {
    @Test
    void normalizesCodesAndProtectsAttributes() {
        final MasterDataRecord record = new MasterDataRecord(UUID.randomUUID(), MasterDataType.CURRENCY,
                " usd ", "US Dollar", null, true, Map.of("numericCode", "840"), 0,
                Instant.parse("2026-07-21T00:00:00Z"), Instant.parse("2026-07-21T00:00:00Z"));

        assertThat(record.code()).isEqualTo("USD");
        assertThat(record.attributes()).containsEntry("numericCode", "840");
    }

    @Test
    void rejectsBlankCode() {
        assertThatThrownBy(() -> new MasterDataRecord(UUID.randomUUID(), MasterDataType.COUNTRY, " ",
                "Iran", null, true, Map.of(), 0, Instant.now(), Instant.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("code is required");
    }

    @Test
    void resolvesTypeSlugs() {
        assertThat(MasterDataType.fromSlug("unit-of-measure")).isEqualTo(MasterDataType.UNIT_OF_MEASURE);
        assertThat(MasterDataType.PRODUCT_CATEGORY.slug()).isEqualTo("product-category");
    }
}
