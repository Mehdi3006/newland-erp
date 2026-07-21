CREATE TABLE master_data_record (
    id uuid PRIMARY KEY,
    aggregate_type varchar(80) NOT NULL,
    code varchar(120) NOT NULL,
    display_name varchar(240) NOT NULL,
    parent_id uuid REFERENCES master_data_record (id),
    active boolean NOT NULL,
    attributes jsonb NOT NULL DEFAULT '{}'::jsonb,
    version bigint NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    CONSTRAINT uq_master_data_type_code UNIQUE (aggregate_type, code),
    CONSTRAINT ck_master_data_version CHECK (version >= 0),
    CONSTRAINT ck_master_data_type CHECK (
        aggregate_type IN (
            'ORGANIZATION',
            'COMPANY',
            'BUSINESS_UNIT',
            'BRANCH',
            'WAREHOUSE',
            'WAREHOUSE_ZONE',
            'WAREHOUSE_BIN',
            'CURRENCY',
            'EXCHANGE_RATE',
            'COUNTRY',
            'PROVINCE',
            'CITY',
            'ADDRESS',
            'UNIT_OF_MEASURE',
            'UNIT_CONVERSION',
            'TAX_CATEGORY',
            'TAX_RATE',
            'PAYMENT_TERMS',
            'PAYMENT_METHOD',
            'SHIPPING_METHOD',
            'INCOTERMS',
            'LANGUAGE',
            'TIME_ZONE',
            'FISCAL_CALENDAR',
            'NUMBER_SERIES',
            'DOCUMENT_TYPE',
            'ATTACHMENT_CATEGORY',
            'PRODUCT_CATEGORY',
            'PRODUCT_BRAND',
            'PRODUCT_FAMILY',
            'PRODUCT_ATTRIBUTE',
            'ATTRIBUTE_VALUE',
            'BARCODE_TYPE'
        )
    )
);

CREATE INDEX ix_master_data_type_active ON master_data_record (aggregate_type, active);
CREATE INDEX ix_master_data_parent ON master_data_record (parent_id);
