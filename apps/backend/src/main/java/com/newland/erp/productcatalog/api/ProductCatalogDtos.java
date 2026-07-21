package com.newland.erp.productcatalog.api;

import com.newland.erp.productcatalog.domain.PackagingLevel;
import com.newland.erp.productcatalog.domain.Product;
import com.newland.erp.productcatalog.domain.ProductContent;
import com.newland.erp.productcatalog.domain.ProductMeasurement;
import com.newland.erp.productcatalog.domain.ProductMedia;
import com.newland.erp.productcatalog.domain.ProductPackage;
import com.newland.erp.productcatalog.domain.ProductSku;
import com.newland.erp.productcatalog.domain.ProductStatus;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ProductCatalogDtos {
    public record MeasurementRequest(@NotNull @PositiveOrZero BigDecimal value, @NotBlank String unitCode,
                                     BigDecimal normalizedValue, String normalizedUnitCode) {
        ProductMeasurement toDomain() {
            return new ProductMeasurement(value, unitCode, normalizedValue, normalizedUnitCode);
        }
    }

    public record SkuRequest(@NotBlank String skuCode, String gtin, String ean, String upc, String barcode,
                             @NotBlank String uomCode, Map<String, String> attributeValues) {
        ProductSku toDomain(final UUID productId) {
            return new ProductSku(UUID.randomUUID(), productId, skuCode, gtin, ean, upc, barcode, uomCode,
                    attributeValues);
        }
    }

    public record PackageRequest(@NotNull PackagingLevel level, @Positive int unitsPerPackage) {
        ProductPackage toDomain() {
            return new ProductPackage(level, unitsPerPackage);
        }
    }

    public record MediaRequest(@NotNull UUID attachmentId, @NotBlank String mediaType, String languageCode,
                               boolean primaryMedia) {
        ProductMedia toDomain() {
            return new ProductMedia(attachmentId, mediaType, languageCode, primaryMedia);
        }
    }

    public record ContentRequest(@NotBlank String languageCode, @NotBlank String displayName, String description,
                                 String manualReference, String brochureReference) {
        ProductContent toDomain() {
            return new ProductContent(languageCode, displayName, description, manualReference, brochureReference);
        }
    }

    public record CreateProductRequest(@NotBlank String productCode, UUID categoryId, UUID brandId, UUID familyId,
                                       List<@Valid SkuRequest> skus, List<@Valid PackageRequest> packagingLevels,
                                       @Valid MeasurementRequest length, @Valid MeasurementRequest width,
                                       @Valid MeasurementRequest height, @Valid MeasurementRequest weight,
                                       List<@Valid MediaRequest> media, List<@Valid ContentRequest> content,
                                       List<String> tags, Map<String, String> searchMetadata,
                                       Map<String, String> warrantyMetadata) {
    }

    public record ChangeStatusRequest(@NotNull ProductStatus status, @PositiveOrZero long expectedVersion) {
    }

    public record ProductResponse(UUID id, String productCode, ProductStatus status, UUID categoryId, UUID brandId,
                                  UUID familyId, List<SkuResponse> skus, List<PackageResponse> packagingLevels,
                                  MeasurementResponse length, MeasurementResponse width, MeasurementResponse height,
                                  MeasurementResponse weight, List<MediaResponse> media,
                                  List<ContentResponse> content, List<String> tags,
                                  Map<String, String> searchMetadata, Map<String, String> warrantyMetadata,
                                  long version, Instant createdAt, Instant updatedAt) {
        static ProductResponse from(final Product product) {
            return new ProductResponse(product.id(), product.productCode(), product.status(), product.categoryId(),
                    product.brandId(), product.familyId(), product.skus().stream().map(SkuResponse::from).toList(),
                    product.packagingLevels().stream().map(PackageResponse::from).toList(),
                    MeasurementResponse.from(product.length()), MeasurementResponse.from(product.width()),
                    MeasurementResponse.from(product.height()), MeasurementResponse.from(product.weight()),
                    product.media().stream().map(MediaResponse::from).toList(),
                    product.content().stream().map(ContentResponse::from).toList(), product.tags(),
                    product.searchMetadata(), product.warrantyMetadata(), product.version(), product.createdAt(),
                    product.updatedAt());
        }
    }

    public record SkuResponse(UUID id, String skuCode, String gtin, String ean, String upc, String barcode,
                              String uomCode, Map<String, String> attributeValues) {
        static SkuResponse from(final ProductSku sku) {
            return new SkuResponse(sku.id(), sku.skuCode(), sku.gtin(), sku.ean(), sku.upc(), sku.barcode(),
                    sku.uomCode(), sku.attributeValues());
        }
    }

    public record PackageResponse(PackagingLevel level, int unitsPerPackage) {
        static PackageResponse from(final ProductPackage pack) {
            return new PackageResponse(pack.level(), pack.unitsPerPackage());
        }
    }

    public record MeasurementResponse(BigDecimal value, String unitCode, BigDecimal normalizedValue,
                                      String normalizedUnitCode) {
        static MeasurementResponse from(final ProductMeasurement measurement) {
            return measurement == null ? null : new MeasurementResponse(measurement.value(), measurement.unitCode(),
                    measurement.normalizedValue(), measurement.normalizedUnitCode());
        }
    }

    public record MediaResponse(UUID attachmentId, String mediaType, String languageCode, boolean primaryMedia) {
        static MediaResponse from(final ProductMedia media) {
            return new MediaResponse(media.attachmentId(), media.mediaType(), media.languageCode(),
                    media.primaryMedia());
        }
    }

    public record ContentResponse(String languageCode, String displayName, String description,
                                  String manualReference, String brochureReference) {
        static ContentResponse from(final ProductContent content) {
            return new ContentResponse(content.languageCode(), content.displayName(), content.description(),
                    content.manualReference(), content.brochureReference());
        }
    }

    private ProductCatalogDtos() {
    }
}
