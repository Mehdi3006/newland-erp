package com.newland.erp.productcatalog.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.newland.erp.productcatalog.application.ProductCatalogRepository;
import com.newland.erp.productcatalog.domain.PackagingLevel;
import com.newland.erp.productcatalog.domain.Product;
import com.newland.erp.productcatalog.domain.ProductContent;
import com.newland.erp.productcatalog.domain.ProductMeasurement;
import com.newland.erp.productcatalog.domain.ProductMedia;
import com.newland.erp.productcatalog.domain.ProductPackage;
import com.newland.erp.productcatalog.domain.ProductSku;
import com.newland.erp.productcatalog.domain.ProductStatus;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.JSONB;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public final class JooqProductCatalogRepository implements ProductCatalogRepository {
    private static final TypeReference<Map<String, String>> STRING_MAP = new TypeReference<>() {
    };
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };
    private final DSLContext dsl;
    private final ObjectMapper objectMapper;

    public JooqProductCatalogRepository(final DSLContext dslContext, final ObjectMapper mapper) {
        this.dsl = dslContext;
        this.objectMapper = mapper;
    }

    @Override
    public Product insert(final Product product) {
        dsl.transaction(configuration -> {
            final DSLContext tx = DSL.using(configuration);
            insertProduct(tx, product);
            replaceChildren(tx, product);
        });
        return product;
    }

    @Override
    public Product update(final Product product) {
        dsl.transaction(configuration -> {
            final DSLContext tx = DSL.using(configuration);
            tx.update(productTable())
                    .set(text("status"), product.status().name())
                    .set(jsonb("tags"), json(product.tags()))
                    .set(jsonb("search_metadata"), json(product.searchMetadata()))
                    .set(jsonb("warranty_metadata"), json(product.warrantyMetadata()))
                    .set(longField("version"), product.version())
                    .set(instant("updated_at"), product.updatedAt())
                    .where(id().eq(product.id()))
                    .execute();
            replaceChildren(tx, product);
        });
        return product;
    }

    @Override
    public Optional<Product> findById(final UUID id) {
        return dsl.selectFrom(productTable()).where(id().eq(id)).fetchOptional(this::product);
    }

    @Override
    public Optional<Product> findByProductCode(final String productCode) {
        return dsl.selectFrom(productTable())
                .where(text("product_code").eq(productCode.toUpperCase()))
                .fetchOptional(this::product);
    }

    @Override
    public boolean skuCodeExists(final String skuCode) {
        return dsl.fetchExists(skuTable(), text("sku_code").eq(skuCode.toUpperCase()));
    }

    @Override
    public boolean tradeIdentifierExists(final String identifier) {
        return dsl.fetchExists(skuTable(), text("gtin").eq(identifier).or(text("ean").eq(identifier))
                .or(text("upc").eq(identifier)).or(text("barcode").eq(identifier)));
    }

    @Override
    public List<Product> listProducts() {
        return dsl.selectFrom(productTable()).orderBy(text("product_code")).fetch(this::product);
    }

    private void insertProduct(final DSLContext tx, final Product product) {
        tx.insertInto(productTable())
                .columns(id(), text("product_code"), text("status"), uuid("category_id"), uuid("brand_id"),
                        uuid("family_id"), decimal("length_value"), text("length_unit"),
                        decimal("length_normalized_value"), text("length_normalized_unit"),
                        decimal("width_value"), text("width_unit"), decimal("width_normalized_value"),
                        text("width_normalized_unit"), decimal("height_value"), text("height_unit"),
                        decimal("height_normalized_value"), text("height_normalized_unit"),
                        decimal("weight_value"), text("weight_unit"), decimal("weight_normalized_value"),
                        text("weight_normalized_unit"), jsonb("tags"), jsonb("search_metadata"),
                        jsonb("warranty_metadata"), longField("version"), instant("created_at"),
                        instant("updated_at"))
                .values(product.id(), product.productCode(), product.status().name(), product.categoryId(),
                        product.brandId(), product.familyId(), value(product.length()), unit(product.length()),
                        normalizedValue(product.length()), normalizedUnit(product.length()), value(product.width()),
                        unit(product.width()), normalizedValue(product.width()), normalizedUnit(product.width()),
                        value(product.height()), unit(product.height()), normalizedValue(product.height()),
                        normalizedUnit(product.height()), value(product.weight()), unit(product.weight()),
                        normalizedValue(product.weight()), normalizedUnit(product.weight()), json(product.tags()),
                        json(product.searchMetadata()), json(product.warrantyMetadata()), product.version(),
                        product.createdAt(), product.updatedAt())
                .execute();
    }

    private void replaceChildren(final DSLContext tx, final Product product) {
        tx.deleteFrom(skuTable()).where(uuid("product_id").eq(product.id())).execute();
        tx.deleteFrom(table("product_catalog_packaging")).where(uuid("product_id").eq(product.id())).execute();
        tx.deleteFrom(table("product_catalog_content")).where(uuid("product_id").eq(product.id())).execute();
        tx.deleteFrom(table("product_catalog_media")).where(uuid("product_id").eq(product.id())).execute();
        product.skus().forEach(sku -> tx.insertInto(skuTable())
                .columns(id(), uuid("product_id"), text("sku_code"), text("gtin"), text("ean"), text("upc"),
                        text("barcode"), text("uom_code"), jsonb("attribute_values"))
                .values(sku.id(), product.id(), sku.skuCode(), sku.gtin(), sku.ean(), sku.upc(), sku.barcode(),
                        sku.uomCode(), json(sku.attributeValues()))
                .execute());
        product.packagingLevels().forEach(pack -> tx.insertInto(table("product_catalog_packaging"))
                .columns(uuid("product_id"), text("level"), integer("units_per_package"))
                .values(product.id(), pack.level().name(), pack.unitsPerPackage())
                .execute());
        product.content().forEach(content -> tx.insertInto(table("product_catalog_content"))
                .columns(uuid("product_id"), text("language_code"), text("display_name"), text("description"),
                        text("manual_reference"), text("brochure_reference"))
                .values(product.id(), content.languageCode(), content.displayName(), content.description(),
                        content.manualReference(), content.brochureReference())
                .execute());
        product.media().forEach(media -> tx.insertInto(table("product_catalog_media"))
                .columns(uuid("product_id"), uuid("attachment_id"), text("media_type"), text("language_code"),
                        bool("primary_media"))
                .values(product.id(), media.attachmentId(), media.mediaType(), media.languageCode(),
                        media.primaryMedia())
                .execute());
    }

    private Product product(final Record record) {
        final UUID productId = record.get(id());
        return new Product(productId, record.get(text("product_code")),
                ProductStatus.valueOf(record.get(text("status"))), record.get(uuid("category_id")),
                record.get(uuid("brand_id")), record.get(uuid("family_id")), skus(productId), packaging(productId),
                measurement(record, "length"), measurement(record, "width"), measurement(record, "height"),
                measurement(record, "weight"), media(productId), content(productId), stringList(record, "tags"),
                stringMap(record, "search_metadata"), stringMap(record, "warranty_metadata"),
                record.get(longField("version")), valueInstant(record, "created_at"),
                valueInstant(record, "updated_at"));
    }

    private List<ProductSku> skus(final UUID productId) {
        return dsl.selectFrom(skuTable()).where(uuid("product_id").eq(productId)).fetch(record ->
                new ProductSku(record.get(id()), record.get(uuid("product_id")), record.get(text("sku_code")),
                        record.get(text("gtin")), record.get(text("ean")), record.get(text("upc")),
                        record.get(text("barcode")), record.get(text("uom_code")),
                        stringMap(record, "attribute_values")));
    }

    private List<ProductPackage> packaging(final UUID productId) {
        return dsl.selectFrom(table("product_catalog_packaging")).where(uuid("product_id").eq(productId))
                .fetch(record -> new ProductPackage(PackagingLevel.valueOf(record.get(text("level"))),
                        record.get(integer("units_per_package"))));
    }

    private List<ProductContent> content(final UUID productId) {
        return dsl.selectFrom(table("product_catalog_content")).where(uuid("product_id").eq(productId))
                .fetch(record -> new ProductContent(record.get(text("language_code")),
                        record.get(text("display_name")), record.get(text("description")),
                        record.get(text("manual_reference")), record.get(text("brochure_reference"))));
    }

    private List<ProductMedia> media(final UUID productId) {
        return dsl.selectFrom(table("product_catalog_media")).where(uuid("product_id").eq(productId))
                .fetch(record -> new ProductMedia(record.get(uuid("attachment_id")), record.get(text("media_type")),
                        record.get(text("language_code")), Boolean.TRUE.equals(record.get(bool("primary_media")))));
    }

    private ProductMeasurement measurement(final Record record, final String prefix) {
        final BigDecimal currentValue = record.get(decimal(prefix + "_value"));
        if (currentValue == null) {
            return null;
        }
        return new ProductMeasurement(currentValue, record.get(text(prefix + "_unit")),
                record.get(decimal(prefix + "_normalized_value")), record.get(text(prefix + "_normalized_unit")));
    }

    private static BigDecimal value(final ProductMeasurement measurement) {
        return measurement == null ? null : measurement.value();
    }

    private static String unit(final ProductMeasurement measurement) {
        return measurement == null ? null : measurement.unitCode();
    }

    private static BigDecimal normalizedValue(final ProductMeasurement measurement) {
        return measurement == null ? null : measurement.normalizedValue();
    }

    private static String normalizedUnit(final ProductMeasurement measurement) {
        return measurement == null ? null : measurement.normalizedUnitCode();
    }

    private JSONB json(final Object value) {
        try {
            return JSONB.valueOf(objectMapper.writeValueAsString(value == null ? Map.of() : value));
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Invalid Product Catalog JSON.", exception);
        }
    }

    private Map<String, String> stringMap(final Record record, final String name) {
        try {
            return objectMapper.readValue(record.get(jsonb(name)).data(), STRING_MAP);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Invalid Product Catalog JSON.", exception);
        }
    }

    private List<String> stringList(final Record record, final String name) {
        try {
            return objectMapper.readValue(record.get(jsonb(name)).data(), STRING_LIST);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Invalid Product Catalog JSON.", exception);
        }
    }

    private static Table<Record> productTable() {
        return table("product_catalog_product");
    }

    private static Table<Record> skuTable() {
        return table("product_catalog_sku");
    }

    private static Table<Record> table(final String name) {
        return DSL.table(DSL.name(name));
    }

    private static Field<UUID> id() {
        return uuid("id");
    }

    private static Field<UUID> uuid(final String name) {
        return DSL.field(DSL.name(name), UUID.class);
    }

    private static Field<String> text(final String name) {
        return DSL.field(DSL.name(name), String.class);
    }

    private static Field<Boolean> bool(final String name) {
        return DSL.field(DSL.name(name), Boolean.class);
    }

    private static Field<Integer> integer(final String name) {
        return DSL.field(DSL.name(name), Integer.class);
    }

    private static Field<Long> longField(final String name) {
        return DSL.field(DSL.name(name), Long.class);
    }

    private static Field<BigDecimal> decimal(final String name) {
        return DSL.field(DSL.name(name), BigDecimal.class);
    }

    private static Field<Instant> instant(final String name) {
        return DSL.field(DSL.name(name), Instant.class);
    }

    private static Field<JSONB> jsonb(final String name) {
        return DSL.field(DSL.name(name), JSONB.class);
    }

    private static Instant valueInstant(final Record record, final String name) {
        final Object value = record.get(DSL.field(DSL.name(name)));
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime.toInstant();
        }
        throw new IllegalStateException("Unsupported timestamp value for " + name + ".");
    }
}
