package com.newland.erp.productcatalog.application;

import com.newland.erp.productcatalog.domain.DuplicateProductIdentifierException;
import com.newland.erp.productcatalog.domain.PackagingLevel;
import com.newland.erp.productcatalog.domain.ProductContent;
import com.newland.erp.productcatalog.domain.ProductMedia;
import com.newland.erp.productcatalog.domain.ProductPackage;
import com.newland.erp.productcatalog.domain.ProductSku;
import com.newland.erp.productcatalog.domain.ProductStatus;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

final class ProductCatalogServiceTest {
    private final InMemoryProductCatalogRepository repository = new InMemoryProductCatalogRepository();
    private final Set<UUID> audited = new HashSet<>();
    private final Set<UUID> attachments = new HashSet<>();
    private final Set<String> languages = new HashSet<>();
    private final ProductCatalogService service = new ProductCatalogService(repository,
            (actor, action, productId) -> audited.add(productId), attachments::add, languages::add,
            Clock.fixed(Instant.parse("2026-07-21T00:00:00Z"), ZoneOffset.UTC));

    @Test
    void createsDraftProductWithSkuPackagingMediaContentAndAudit() {
        final UUID attachmentId = UUID.randomUUID();
        final var product = service.create(command("P-100", "SKU-100", "1234567890123", attachmentId));

        assertThat(product.status()).isEqualTo(ProductStatus.DRAFT);
        assertThat(product.skus()).hasSize(1);
        assertThat(product.skus().getFirst().productId()).isEqualTo(product.id());
        assertThat(product.packagingLevels()).extracting(ProductPackage::level)
                .containsExactly(PackagingLevel.UNIT, PackagingLevel.INNER_PACK, PackagingLevel.CARTON,
                        PackagingLevel.PALLET);
        assertThat(audited).containsExactly(product.id());
        assertThat(attachments).containsExactly(attachmentId);
        assertThat(languages).containsExactly("en");
    }

    @Test
    void rejectsDuplicateSkuCodesAndTradeIdentifiers() {
        service.create(command("P-100", "SKU-100", "1234567890123", UUID.randomUUID()));

        assertThatThrownBy(() -> service.create(command("P-101", "SKU-100", null, UUID.randomUUID())))
                .isInstanceOf(DuplicateProductIdentifierException.class);
        assertThatThrownBy(() -> service.create(command("P-102", "SKU-102", "1234567890123", UUID.randomUUID())))
                .isInstanceOf(DuplicateProductIdentifierException.class);
    }

    @Test
    void changesLifecycleStatusWithOptimisticVersion() {
        final var product = service.create(command("P-100", "SKU-100", null, UUID.randomUUID()));

        final var active = service.changeStatus(new ProductCatalogCommands.ChangeStatus(product.id(),
                ProductStatus.ACTIVE, product.version(), "architect"));

        assertThat(active.status()).isEqualTo(ProductStatus.ACTIVE);
        assertThat(active.version()).isEqualTo(1);
    }

    private static ProductCatalogCommands.CreateProduct command(final String productCode, final String skuCode,
                                                                final String gtin, final UUID attachmentId) {
        return new ProductCatalogCommands.CreateProduct(productCode, UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), List.of(new ProductSku(UUID.randomUUID(), UUID.randomUUID(), skuCode, gtin,
                null, null, null, "EA", Map.of("color", "black"))),
                List.of(new ProductPackage(PackagingLevel.UNIT, 1),
                        new ProductPackage(PackagingLevel.INNER_PACK, 6),
                        new ProductPackage(PackagingLevel.CARTON, 24),
                        new ProductPackage(PackagingLevel.PALLET, 480)),
                null, null, null, null, List.of(new ProductMedia(attachmentId, "IMAGE", "en", true)),
                List.of(new ProductContent("en", "Pump", "Catalog description", "manual-ref", "brochure-ref")),
                List.of("industrial"), Map.of("keywords", "pump"), Map.of("months", "24"), "architect");
    }
}
