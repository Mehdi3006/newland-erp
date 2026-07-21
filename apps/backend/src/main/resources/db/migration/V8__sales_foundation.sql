CREATE TABLE sales_customer (
    id uuid PRIMARY KEY,
    idempotency_key varchar(160) NOT NULL UNIQUE,
    customer_code varchar(120) NOT NULL UNIQUE,
    name varchar(240) NOT NULL,
    status varchar(24) NOT NULL,
    created_at timestamptz NOT NULL,
    CONSTRAINT ck_sales_customer_status CHECK (status IN ('PROSPECT', 'ACTIVE', 'ON_HOLD', 'INACTIVE', 'BLOCKED'))
);

CREATE TABLE sales_customer_contact (
    id uuid PRIMARY KEY,
    customer_id uuid NOT NULL REFERENCES sales_customer (id),
    name varchar(160) NOT NULL,
    email varchar(240),
    phone varchar(80)
);

CREATE TABLE sales_customer_address (
    id uuid PRIMARY KEY,
    customer_id uuid NOT NULL REFERENCES sales_customer (id),
    country_id uuid NOT NULL,
    province_id uuid,
    city_id uuid,
    address_line varchar(500) NOT NULL
);

CREATE TABLE sales_customer_credit_profile (
    id uuid PRIMARY KEY,
    customer_id uuid NOT NULL REFERENCES sales_customer (id),
    company_id uuid NOT NULL,
    currency_id uuid NOT NULL,
    credit_limit numeric(19, 6) NOT NULL,
    credit_hold boolean NOT NULL,
    CONSTRAINT uq_sales_customer_credit_company UNIQUE (customer_id, company_id),
    CONSTRAINT ck_sales_customer_credit_limit CHECK (credit_limit >= 0)
);

CREATE TABLE sales_customer_product_reference (
    id uuid PRIMARY KEY,
    customer_id uuid NOT NULL REFERENCES sales_customer (id),
    product_id uuid NOT NULL,
    sku_id uuid NOT NULL,
    customer_sku varchar(120) NOT NULL,
    CONSTRAINT uq_sales_customer_sku UNIQUE (customer_id, sku_id)
);

CREATE TABLE sales_quotation (
    id uuid PRIMARY KEY,
    quotation_number varchar(120) NOT NULL UNIQUE,
    idempotency_key varchar(160) NOT NULL UNIQUE,
    customer_id uuid NOT NULL REFERENCES sales_customer (id),
    company_id uuid NOT NULL,
    branch_id uuid NOT NULL,
    warehouse_id uuid NOT NULL,
    sales_channel_id uuid NOT NULL,
    currency_id uuid NOT NULL,
    payment_terms_id uuid,
    shipping_method_id uuid,
    incoterms_id uuid,
    status varchar(24) NOT NULL,
    revision integer NOT NULL,
    lock_version integer NOT NULL DEFAULT 0,
    expires_on date,
    created_at timestamptz NOT NULL,
    actor varchar(160) NOT NULL,
    CONSTRAINT ck_sales_quotation_status CHECK
        (status IN ('DRAFT', 'SUBMITTED', 'APPROVED', 'REJECTED', 'EXPIRED', 'REVISED', 'CONVERTED')),
    CONSTRAINT ck_sales_quotation_revision CHECK (revision >= 0),
    CONSTRAINT ck_sales_quotation_lock_version CHECK (lock_version >= 0)
);

CREATE TABLE sales_quotation_line (
    id uuid PRIMARY KEY,
    quotation_id uuid NOT NULL REFERENCES sales_quotation (id),
    product_id uuid NOT NULL,
    sku_id uuid NOT NULL,
    sku_code varchar(120) NOT NULL,
    quantity numeric(19, 6) NOT NULL,
    uom_code varchar(32) NOT NULL,
    unit_price numeric(19, 6),
    tax_category_id uuid,
    CONSTRAINT ck_sales_quotation_line_quantity CHECK (quantity > 0)
);

CREATE TABLE sales_quotation_revision (
    id uuid PRIMARY KEY,
    quotation_id uuid NOT NULL REFERENCES sales_quotation (id),
    revision integer NOT NULL,
    reason varchar(500) NOT NULL,
    created_at timestamptz NOT NULL,
    actor varchar(160) NOT NULL,
    CONSTRAINT uq_sales_quotation_revision UNIQUE (quotation_id, revision)
);

CREATE TABLE sales_order (
    id uuid PRIMARY KEY,
    order_number varchar(120) NOT NULL UNIQUE,
    idempotency_key varchar(160) NOT NULL UNIQUE,
    quotation_id uuid REFERENCES sales_quotation (id),
    customer_id uuid NOT NULL REFERENCES sales_customer (id),
    company_id uuid NOT NULL,
    branch_id uuid NOT NULL,
    warehouse_id uuid NOT NULL,
    sales_channel_id uuid NOT NULL,
    currency_id uuid NOT NULL,
    status varchar(32) NOT NULL,
    revision integer NOT NULL,
    lock_version integer NOT NULL DEFAULT 0,
    requested_delivery_date date,
    created_at timestamptz NOT NULL,
    actor varchar(160) NOT NULL,
    CONSTRAINT ck_sales_order_status CHECK
        (status IN ('DRAFT', 'APPROVED', 'PARTIALLY_RESERVED', 'PARTIALLY_DELIVERED', 'DELIVERED', 'CANCELLED')),
    CONSTRAINT ck_sales_order_revision CHECK (revision >= 0),
    CONSTRAINT ck_sales_order_lock_version CHECK (lock_version >= 0)
);

CREATE TABLE sales_order_line (
    id uuid PRIMARY KEY,
    sales_order_id uuid NOT NULL REFERENCES sales_order (id),
    product_id uuid NOT NULL,
    sku_id uuid NOT NULL,
    sku_code varchar(120) NOT NULL,
    ordered_quantity numeric(19, 6) NOT NULL,
    reserved_quantity numeric(19, 6) NOT NULL,
    delivered_quantity numeric(19, 6) NOT NULL,
    cancelled_quantity numeric(19, 6) NOT NULL,
    uom_code varchar(32) NOT NULL,
    tax_category_id uuid,
    CONSTRAINT ck_sales_order_line_ordered CHECK (ordered_quantity > 0),
    CONSTRAINT ck_sales_order_line_reserved CHECK (reserved_quantity >= 0),
    CONSTRAINT ck_sales_order_line_delivered CHECK (delivered_quantity >= 0),
    CONSTRAINT ck_sales_order_line_cancelled CHECK (cancelled_quantity >= 0),
    CONSTRAINT ck_sales_order_line_remaining CHECK
        ((reserved_quantity + delivered_quantity + cancelled_quantity) <= ordered_quantity)
);

CREATE TABLE sales_order_revision (
    id uuid PRIMARY KEY,
    sales_order_id uuid NOT NULL REFERENCES sales_order (id),
    revision integer NOT NULL,
    reason varchar(500) NOT NULL,
    created_at timestamptz NOT NULL,
    actor varchar(160) NOT NULL,
    CONSTRAINT uq_sales_order_revision UNIQUE (sales_order_id, revision)
);

CREATE INDEX ix_sales_quotation_customer ON sales_quotation (customer_id);
CREATE INDEX ix_sales_order_customer ON sales_order (customer_id);
