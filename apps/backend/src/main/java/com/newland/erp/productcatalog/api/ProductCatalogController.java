package com.newland.erp.productcatalog.api;

import com.newland.erp.productcatalog.application.ProductCatalogCommands;
import com.newland.erp.productcatalog.application.ProductCatalogService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/product-catalog")
public final class ProductCatalogController {
    private static final String ACTOR_HEADER = "X-Newland-Actor";
    private final ProductCatalogService service;

    public ProductCatalogController(final ProductCatalogService productCatalogService) {
        this.service = productCatalogService;
    }

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductCatalogDtos.ProductResponse createProduct(
            @Valid @RequestBody final ProductCatalogDtos.CreateProductRequest request,
            @RequestHeader(name = ACTOR_HEADER, defaultValue = "system") final String actor
    ) {
        final UUID transientProductId = UUID.randomUUID();
        return ProductCatalogDtos.ProductResponse.from(service.create(new ProductCatalogCommands.CreateProduct(
                request.productCode(), request.categoryId(), request.brandId(), request.familyId(),
                list(request.skus()).stream().map(sku -> sku.toDomain(transientProductId)).toList(),
                list(request.packagingLevels()).stream().map(ProductCatalogDtos.PackageRequest::toDomain).toList(),
                request.length() == null ? null : request.length().toDomain(),
                request.width() == null ? null : request.width().toDomain(),
                request.height() == null ? null : request.height().toDomain(),
                request.weight() == null ? null : request.weight().toDomain(),
                list(request.media()).stream().map(ProductCatalogDtos.MediaRequest::toDomain).toList(),
                list(request.content()).stream().map(ProductCatalogDtos.ContentRequest::toDomain).toList(),
                request.tags(), request.searchMetadata(), request.warrantyMetadata(), actor)));
    }

    @GetMapping("/products")
    public List<ProductCatalogDtos.ProductResponse> listProducts() {
        return service.list().stream().map(ProductCatalogDtos.ProductResponse::from).toList();
    }

    @GetMapping("/products/{productId}")
    public ProductCatalogDtos.ProductResponse getProduct(@PathVariable final UUID productId) {
        return ProductCatalogDtos.ProductResponse.from(service.get(productId));
    }

    @PutMapping("/products/{productId}/status")
    public ProductCatalogDtos.ProductResponse changeStatus(
            @PathVariable final UUID productId,
            @Valid @RequestBody final ProductCatalogDtos.ChangeStatusRequest request,
            @RequestHeader(name = ACTOR_HEADER, defaultValue = "system") final String actor
    ) {
        return ProductCatalogDtos.ProductResponse.from(service.changeStatus(new ProductCatalogCommands.ChangeStatus(
                productId, request.status(), request.expectedVersion(), actor)));
    }

    private static <T> List<T> list(final List<T> value) {
        return value == null ? List.of() : value;
    }
}
