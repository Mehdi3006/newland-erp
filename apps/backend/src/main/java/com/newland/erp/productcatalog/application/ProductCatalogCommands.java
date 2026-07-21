package com.newland.erp.productcatalog.application;

import com.newland.erp.productcatalog.domain.ProductContent;
import com.newland.erp.productcatalog.domain.ProductMedia;
import com.newland.erp.productcatalog.domain.ProductMeasurement;
import com.newland.erp.productcatalog.domain.ProductPackage;
import com.newland.erp.productcatalog.domain.ProductSku;
import com.newland.erp.productcatalog.domain.ProductStatus;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ProductCatalogCommands {
    public record CreateProduct(String productCode, UUID categoryId, UUID brandId, UUID familyId,
                                List<ProductSku> skus, List<ProductPackage> packagingLevels,
                                ProductMeasurement length, ProductMeasurement width, ProductMeasurement height,
                                ProductMeasurement weight, List<ProductMedia> media, List<ProductContent> content,
                                List<String> tags, Map<String, String> searchMetadata,
                                Map<String, String> warrantyMetadata, String actor) {
        public CreateProduct {
            skus = skus == null ? List.of() : List.copyOf(skus);
            packagingLevels = packagingLevels == null ? List.of() : List.copyOf(packagingLevels);
            media = media == null ? List.of() : List.copyOf(media);
            content = content == null ? List.of() : List.copyOf(content);
            tags = tags == null ? List.of() : List.copyOf(tags);
            searchMetadata = searchMetadata == null ? Map.of() : Map.copyOf(searchMetadata);
            warrantyMetadata = warrantyMetadata == null ? Map.of() : Map.copyOf(warrantyMetadata);
        }
    }

    public record ChangeStatus(UUID productId, ProductStatus status, long expectedVersion, String actor) {
    }

    private ProductCatalogCommands() {
    }
}
