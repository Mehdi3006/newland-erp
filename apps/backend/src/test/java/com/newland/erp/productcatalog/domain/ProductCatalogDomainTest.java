package com.newland.erp.productcatalog.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

final class ProductCatalogDomainTest {
    @Test
    void normalizesProductSkuAndMeasurementValues() {
        final UUID productId = UUID.randomUUID();
        final ProductSku sku = new ProductSku(UUID.randomUUID(), productId, " sku-1 ", " gtin1 ",
                null, null, " barcode1 ", " ea ", Map.of("color", "black"));
        final ProductMeasurement weight = new ProductMeasurement(new BigDecimal("2.5"), "kg",
                new BigDecimal("2500"), "g");

        assertThat(sku.skuCode()).isEqualTo("SKU-1");
        assertThat(sku.barcode()).isEqualTo("BARCODE1");
        assertThat(weight.normalizedUnitCode()).isEqualTo("G");
    }

    @Test
    void supportsPackagingHierarchyAndLifecycleStatus() {
        final Product product = new Product(UUID.randomUUID(), "P-100", ProductStatus.DRAFT,
                null, null, null, List.of(), List.of(new ProductPackage(PackagingLevel.UNIT, 1),
                new ProductPackage(PackagingLevel.CARTON, 12), new ProductPackage(PackagingLevel.PALLET, 480)),
                null, null, null, null, List.of(), List.of(), List.of("durable"), Map.of("keyword", "pump"),
                Map.of("months", "24"), 0, Instant.parse("2026-07-21T00:00:00Z"),
                Instant.parse("2026-07-21T00:00:00Z"));

        assertThat(product.withStatus(ProductStatus.ACTIVE, Instant.parse("2026-07-21T01:00:00Z")).status())
                .isEqualTo(ProductStatus.ACTIVE);
        assertThat(product.packagingLevels()).extracting(ProductPackage::level)
                .containsExactly(PackagingLevel.UNIT, PackagingLevel.CARTON, PackagingLevel.PALLET);
    }

    @Test
    void rejectsNegativeMeasurementsAndInvalidPackageUnits() {
        assertThatThrownBy(() -> new ProductMeasurement(new BigDecimal("-1"), "kg", null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ProductPackage(PackagingLevel.CARTON, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
