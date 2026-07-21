package com.newland.erp.productcatalog.application;

import com.newland.erp.productcatalog.domain.DuplicateProductIdentifierException;
import com.newland.erp.productcatalog.domain.Product;
import com.newland.erp.productcatalog.domain.ProductMedia;
import com.newland.erp.productcatalog.domain.ProductNotFoundException;
import com.newland.erp.productcatalog.domain.ProductSku;
import com.newland.erp.productcatalog.domain.ProductStatus;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public final class ProductCatalogService {
    private final ProductCatalogRepository repository;
    private final ProductCatalogPorts.AuditPort auditPort;
    private final ProductCatalogPorts.AttachmentPort attachmentPort;
    private final ProductCatalogPorts.LocalizationPort localizationPort;
    private final Clock clock;

    public ProductCatalogService(final ProductCatalogRepository productRepository,
                                 final ProductCatalogPorts.AuditPort audit,
                                 final ProductCatalogPorts.AttachmentPort attachments,
                                 final ProductCatalogPorts.LocalizationPort localization,
                                 final Clock systemClock) {
        this.repository = productRepository;
        this.auditPort = audit;
        this.attachmentPort = attachments;
        this.localizationPort = localization;
        this.clock = systemClock;
    }

    @Transactional
    public Product create(final ProductCatalogCommands.CreateProduct command) {
        repository.findByProductCode(command.productCode()).ifPresent(product -> {
            throw new DuplicateProductIdentifierException("Product code already exists: " + product.productCode());
        });
        command.skus().forEach(sku -> {
            if (repository.skuCodeExists(sku.skuCode())) {
                throw new DuplicateProductIdentifierException("SKU code already exists: " + sku.skuCode());
            }
            for (final String identifier : new String[] {sku.gtin(), sku.ean(), sku.upc(), sku.barcode()}) {
                if (identifier != null && repository.tradeIdentifierExists(identifier)) {
                    throw new DuplicateProductIdentifierException("Product identifier already exists: " + identifier);
                }
            }
        });
        command.media().stream().map(ProductMedia::attachmentId).forEach(attachmentPort::requireAttachment);
        command.content().forEach(content -> localizationPort.requireLanguage(content.languageCode()));
        final Instant now = Instant.now(clock);
        final UUID productId = UUID.randomUUID();
        final List<ProductSku> skus = command.skus().stream()
                .map(sku -> new ProductSku(sku.id(), productId, sku.skuCode(), sku.gtin(), sku.ean(),
                        sku.upc(), sku.barcode(), sku.uomCode(), sku.attributeValues()))
                .toList();
        final Product product = new Product(productId, command.productCode(), ProductStatus.DRAFT,
                command.categoryId(), command.brandId(), command.familyId(), skus,
                command.packagingLevels(), command.length(), command.width(), command.height(), command.weight(),
                command.media(), command.content(), command.tags(), command.searchMetadata(),
                command.warrantyMetadata(), 0, now, now);
        final Product saved = repository.insert(product);
        auditPort.record(command.actor(), "PRODUCT_CREATED", saved.id());
        return saved;
    }

    @Transactional
    public Product changeStatus(final ProductCatalogCommands.ChangeStatus command) {
        final Product existing = get(command.productId());
        if (existing.version() != command.expectedVersion()) {
            throw new IllegalArgumentException("Product version conflict: " + command.productId());
        }
        final Product saved = repository.update(existing.withStatus(command.status(), Instant.now(clock)));
        auditPort.record(command.actor(), "PRODUCT_STATUS_CHANGED", saved.id());
        return saved;
    }

    @Transactional(readOnly = true)
    public Product get(final UUID productId) {
        return repository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found: " + productId));
    }

    @Transactional(readOnly = true)
    public List<Product> list() {
        return repository.listProducts();
    }
}
