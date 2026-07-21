CREATE TABLE product_catalog_product (
    id uuid PRIMARY KEY,
    product_code varchar(120) NOT NULL,
    status varchar(24) NOT NULL,
    category_id uuid,
    brand_id uuid,
    family_id uuid,
    length_value numeric(19, 6),
    length_unit varchar(24),
    length_normalized_value numeric(19, 6),
    length_normalized_unit varchar(24),
    width_value numeric(19, 6),
    width_unit varchar(24),
    width_normalized_value numeric(19, 6),
    width_normalized_unit varchar(24),
    height_value numeric(19, 6),
    height_unit varchar(24),
    height_normalized_value numeric(19, 6),
    height_normalized_unit varchar(24),
    weight_value numeric(19, 6),
    weight_unit varchar(24),
    weight_normalized_value numeric(19, 6),
    weight_normalized_unit varchar(24),
    tags jsonb NOT NULL DEFAULT '[]'::jsonb,
    search_metadata jsonb NOT NULL DEFAULT '{}'::jsonb,
    warranty_metadata jsonb NOT NULL DEFAULT '{}'::jsonb,
    version bigint NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    CONSTRAINT uq_product_catalog_product_code UNIQUE (product_code),
    CONSTRAINT ck_product_catalog_status CHECK (status IN ('DRAFT', 'ACTIVE', 'INACTIVE', 'DISCONTINUED')),
    CONSTRAINT ck_product_catalog_version CHECK (version >= 0)
);

CREATE TABLE product_catalog_sku (
    id uuid PRIMARY KEY,
    product_id uuid NOT NULL REFERENCES product_catalog_product (id) ON DELETE CASCADE,
    sku_code varchar(120) NOT NULL,
    gtin varchar(32),
    ean varchar(32),
    upc varchar(32),
    barcode varchar(80),
    uom_code varchar(32) NOT NULL,
    attribute_values jsonb NOT NULL DEFAULT '{}'::jsonb,
    CONSTRAINT uq_product_catalog_sku_code UNIQUE (sku_code),
    CONSTRAINT uq_product_catalog_sku_gtin UNIQUE (gtin),
    CONSTRAINT uq_product_catalog_sku_ean UNIQUE (ean),
    CONSTRAINT uq_product_catalog_sku_upc UNIQUE (upc),
    CONSTRAINT uq_product_catalog_sku_barcode UNIQUE (barcode)
);

CREATE TABLE product_catalog_packaging (
    product_id uuid NOT NULL REFERENCES product_catalog_product (id) ON DELETE CASCADE,
    level varchar(24) NOT NULL,
    units_per_package integer NOT NULL,
    PRIMARY KEY (product_id, level),
    CONSTRAINT ck_product_catalog_packaging_level CHECK (level IN ('UNIT', 'INNER_PACK', 'CARTON', 'PALLET')),
    CONSTRAINT ck_product_catalog_units_per_package CHECK (units_per_package > 0)
);

CREATE TABLE product_catalog_content (
    product_id uuid NOT NULL REFERENCES product_catalog_product (id) ON DELETE CASCADE,
    language_code varchar(16) NOT NULL,
    display_name varchar(240) NOT NULL,
    description text NOT NULL,
    manual_reference varchar(240),
    brochure_reference varchar(240),
    PRIMARY KEY (product_id, language_code)
);

CREATE TABLE product_catalog_media (
    product_id uuid NOT NULL REFERENCES product_catalog_product (id) ON DELETE CASCADE,
    attachment_id uuid NOT NULL,
    media_type varchar(40) NOT NULL,
    language_code varchar(16),
    primary_media boolean NOT NULL DEFAULT false,
    PRIMARY KEY (product_id, attachment_id)
);

CREATE INDEX ix_product_catalog_status ON product_catalog_product (status);
CREATE INDEX ix_product_catalog_category ON product_catalog_product (category_id);
CREATE INDEX ix_product_catalog_brand ON product_catalog_product (brand_id);
CREATE INDEX ix_product_catalog_family ON product_catalog_product (family_id);
CREATE INDEX ix_product_catalog_sku_product ON product_catalog_sku (product_id);
CREATE INDEX ix_product_catalog_media_product ON product_catalog_media (product_id);
