CREATE TABLE procurement_supplier (
    id uuid PRIMARY KEY,
    idempotency_key varchar(160) NOT NULL UNIQUE,
    supplier_code varchar(120) NOT NULL UNIQUE,
    name varchar(240) NOT NULL,
    status varchar(24) NOT NULL,
    created_at timestamptz NOT NULL,
    CONSTRAINT ck_procurement_supplier_status CHECK (status IN ('DRAFT', 'ACTIVE', 'INACTIVE', 'BLOCKED'))
);

CREATE TABLE procurement_supplier_contact (
    id uuid PRIMARY KEY,
    supplier_id uuid NOT NULL REFERENCES procurement_supplier (id),
    name varchar(160) NOT NULL,
    email varchar(240),
    phone varchar(80)
);

CREATE TABLE procurement_supplier_address (
    id uuid PRIMARY KEY,
    supplier_id uuid NOT NULL REFERENCES procurement_supplier (id),
    country_id uuid NOT NULL,
    province_id uuid,
    city_id uuid,
    address_line varchar(500) NOT NULL
);

CREATE TABLE procurement_supplier_product_reference (
    id uuid PRIMARY KEY,
    supplier_id uuid NOT NULL REFERENCES procurement_supplier (id),
    product_id uuid NOT NULL,
    sku_id uuid NOT NULL,
    supplier_sku varchar(120) NOT NULL,
    lead_time_days integer NOT NULL,
    minimum_order_quantity numeric(19, 6) NOT NULL,
    uom_code varchar(32) NOT NULL,
    packaging_information varchar(500),
    CONSTRAINT uq_procurement_supplier_sku UNIQUE (supplier_id, sku_id, supplier_sku),
    CONSTRAINT ck_procurement_supplier_lead_time CHECK (lead_time_days >= 0),
    CONSTRAINT ck_procurement_supplier_moq CHECK (minimum_order_quantity > 0)
);

CREATE TABLE procurement_purchase_requisition (
    id uuid PRIMARY KEY,
    requisition_number varchar(120) NOT NULL UNIQUE,
    idempotency_key varchar(160) NOT NULL UNIQUE,
    company_id uuid NOT NULL,
    branch_id uuid NOT NULL,
    warehouse_id uuid NOT NULL,
    status varchar(24) NOT NULL,
    revision integer NOT NULL,
    created_at timestamptz NOT NULL,
    actor varchar(160) NOT NULL,
    CONSTRAINT ck_procurement_requisition_status CHECK
        (status IN ('DRAFT', 'SUBMITTED', 'APPROVED', 'REJECTED', 'REVISED')),
    CONSTRAINT ck_procurement_requisition_revision CHECK (revision >= 0)
);

CREATE TABLE procurement_purchase_requisition_line (
    id uuid PRIMARY KEY,
    requisition_id uuid NOT NULL REFERENCES procurement_purchase_requisition (id),
    product_id uuid NOT NULL,
    sku_id uuid NOT NULL,
    sku_code varchar(120) NOT NULL,
    quantity numeric(19, 6) NOT NULL,
    uom_code varchar(32) NOT NULL,
    unit_price numeric(19, 6),
    tax_category_id uuid,
    CONSTRAINT ck_procurement_requisition_line_quantity CHECK (quantity > 0)
);

CREATE TABLE procurement_rfq (
    id uuid PRIMARY KEY,
    rfq_number varchar(120) NOT NULL UNIQUE,
    idempotency_key varchar(160) NOT NULL UNIQUE,
    requisition_id uuid NOT NULL REFERENCES procurement_purchase_requisition (id),
    status varchar(24) NOT NULL,
    created_at timestamptz NOT NULL,
    CONSTRAINT ck_procurement_rfq_status CHECK (status IN ('DRAFT', 'SENT', 'CLOSED'))
);

CREATE TABLE procurement_rfq_supplier_invitation (
    id uuid PRIMARY KEY,
    rfq_id uuid NOT NULL REFERENCES procurement_rfq (id),
    supplier_id uuid NOT NULL REFERENCES procurement_supplier (id),
    CONSTRAINT uq_procurement_rfq_supplier UNIQUE (rfq_id, supplier_id)
);

CREATE TABLE procurement_supplier_quotation (
    id uuid PRIMARY KEY,
    quotation_number varchar(120) NOT NULL UNIQUE,
    idempotency_key varchar(160) NOT NULL UNIQUE,
    rfq_id uuid NOT NULL REFERENCES procurement_rfq (id),
    supplier_id uuid NOT NULL REFERENCES procurement_supplier (id),
    currency_id uuid NOT NULL,
    payment_terms_id uuid,
    shipping_method_id uuid,
    incoterms_id uuid,
    status varchar(24) NOT NULL,
    submitted_at timestamptz NOT NULL,
    CONSTRAINT ck_procurement_quotation_status CHECK
        (status IN ('SUBMITTED', 'COMPARED', 'ACCEPTED', 'REJECTED'))
);

CREATE TABLE procurement_supplier_quotation_line (
    id uuid PRIMARY KEY,
    quotation_id uuid NOT NULL REFERENCES procurement_supplier_quotation (id),
    product_id uuid NOT NULL,
    sku_id uuid NOT NULL,
    sku_code varchar(120) NOT NULL,
    quantity numeric(19, 6) NOT NULL,
    uom_code varchar(32) NOT NULL,
    unit_price numeric(19, 6),
    tax_category_id uuid,
    CONSTRAINT ck_procurement_quotation_line_quantity CHECK (quantity > 0)
);

CREATE TABLE procurement_quotation_comparison (
    id uuid PRIMARY KEY,
    rfq_id uuid NOT NULL REFERENCES procurement_rfq (id),
    selected_quotation_id uuid NOT NULL REFERENCES procurement_supplier_quotation (id),
    compared_quotation_ids text NOT NULL,
    compared_at timestamptz NOT NULL,
    actor varchar(160) NOT NULL
);

CREATE TABLE procurement_purchase_order (
    id uuid PRIMARY KEY,
    order_number varchar(120) NOT NULL UNIQUE,
    idempotency_key varchar(160) NOT NULL UNIQUE,
    requisition_id uuid REFERENCES procurement_purchase_requisition (id),
    supplier_id uuid NOT NULL REFERENCES procurement_supplier (id),
    company_id uuid NOT NULL,
    branch_id uuid NOT NULL,
    warehouse_id uuid NOT NULL,
    currency_id uuid NOT NULL,
    status varchar(32) NOT NULL,
    revision integer NOT NULL,
    expected_delivery_date date,
    created_at timestamptz NOT NULL,
    actor varchar(160) NOT NULL,
    CONSTRAINT ck_procurement_po_status CHECK
        (status IN ('DRAFT', 'APPROVED', 'PARTIALLY_RECEIVED', 'RECEIVED', 'CANCELLED')),
    CONSTRAINT ck_procurement_po_revision CHECK (revision >= 0)
);

CREATE TABLE procurement_purchase_order_line (
    id uuid PRIMARY KEY,
    purchase_order_id uuid NOT NULL REFERENCES procurement_purchase_order (id),
    product_id uuid NOT NULL,
    sku_id uuid NOT NULL,
    sku_code varchar(120) NOT NULL,
    ordered_quantity numeric(19, 6) NOT NULL,
    received_quantity numeric(19, 6) NOT NULL,
    cancelled_quantity numeric(19, 6) NOT NULL,
    uom_code varchar(32) NOT NULL,
    tax_category_id uuid,
    CONSTRAINT ck_procurement_po_line_ordered CHECK (ordered_quantity > 0),
    CONSTRAINT ck_procurement_po_line_received CHECK (received_quantity >= 0),
    CONSTRAINT ck_procurement_po_line_cancelled CHECK (cancelled_quantity >= 0),
    CONSTRAINT ck_procurement_po_line_remaining CHECK
        ((received_quantity + cancelled_quantity) <= ordered_quantity)
);

CREATE TABLE procurement_purchase_order_revision (
    id uuid PRIMARY KEY,
    purchase_order_id uuid NOT NULL REFERENCES procurement_purchase_order (id),
    revision integer NOT NULL,
    reason varchar(500) NOT NULL,
    created_at timestamptz NOT NULL,
    actor varchar(160) NOT NULL,
    CONSTRAINT uq_procurement_po_revision UNIQUE (purchase_order_id, revision)
);

CREATE INDEX ix_procurement_requisition_company ON procurement_purchase_requisition (company_id, branch_id);
CREATE INDEX ix_procurement_po_supplier ON procurement_purchase_order (supplier_id);
