CREATE TABLE inventory_stock_transaction (
    id uuid PRIMARY KEY,
    transaction_number varchar(120) NOT NULL UNIQUE,
    idempotency_key varchar(160) NOT NULL UNIQUE,
    movement_type varchar(40) NOT NULL,
    status varchar(24) NOT NULL,
    reversed_transaction_id uuid,
    posted_at timestamptz NOT NULL,
    business_date date NOT NULL,
    actor varchar(160) NOT NULL,
    CONSTRAINT ck_inventory_transaction_status CHECK (status IN ('POSTED', 'REVERSED'))
);

CREATE TABLE inventory_stock_movement_line (
    id uuid PRIMARY KEY,
    transaction_id uuid NOT NULL REFERENCES inventory_stock_transaction (id),
    product_id uuid NOT NULL,
    sku_id uuid NOT NULL,
    sku_code varchar(120) NOT NULL,
    uom_code varchar(32) NOT NULL,
    tracking_policy varchar(16) NOT NULL,
    from_warehouse_id uuid,
    from_zone_id uuid,
    from_bin_id uuid,
    to_warehouse_id uuid,
    to_zone_id uuid,
    to_bin_id uuid,
    quantity numeric(19, 6) NOT NULL,
    inventory_status varchar(24) NOT NULL,
    lot_code varchar(120),
    serial_code varchar(120),
    expiry_date date,
    CONSTRAINT ck_inventory_line_quantity CHECK (quantity > 0)
);

CREATE TABLE inventory_stock_ledger_entry (
    id uuid PRIMARY KEY,
    transaction_id uuid NOT NULL REFERENCES inventory_stock_transaction (id),
    line_id uuid NOT NULL REFERENCES inventory_stock_movement_line (id),
    sku_id uuid NOT NULL,
    warehouse_id uuid NOT NULL,
    zone_id uuid,
    bin_id uuid,
    quantity_delta numeric(19, 6) NOT NULL,
    uom_code varchar(32) NOT NULL,
    inventory_status varchar(24) NOT NULL,
    lot_code varchar(120),
    serial_code varchar(120),
    expiry_date date,
    posted_at timestamptz NOT NULL
);

CREATE TABLE inventory_stock_balance (
    id uuid PRIMARY KEY,
    sku_id uuid NOT NULL,
    warehouse_id uuid NOT NULL,
    zone_id uuid,
    bin_id uuid,
    inventory_status varchar(24) NOT NULL,
    on_hand_quantity numeric(19, 6) NOT NULL,
    reserved_quantity numeric(19, 6) NOT NULL,
    in_transit_quantity numeric(19, 6) NOT NULL,
    damaged_quantity numeric(19, 6) NOT NULL,
    quarantine_quantity numeric(19, 6) NOT NULL,
    uom_code varchar(32) NOT NULL,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT uq_inventory_balance_scope UNIQUE NULLS NOT DISTINCT
        (sku_id, warehouse_id, zone_id, bin_id, inventory_status),
    CONSTRAINT ck_inventory_balance_version CHECK (version >= 0)
);

CREATE TABLE inventory_reservation (
    id uuid PRIMARY KEY,
    sku_id uuid NOT NULL,
    warehouse_id uuid NOT NULL,
    zone_id uuid,
    bin_id uuid,
    quantity numeric(19, 6) NOT NULL,
    uom_code varchar(32) NOT NULL,
    released boolean NOT NULL,
    created_at timestamptz NOT NULL,
    released_at timestamptz,
    CONSTRAINT ck_inventory_reservation_quantity CHECK (quantity > 0)
);

CREATE TABLE inventory_lot (
    id uuid PRIMARY KEY,
    sku_id uuid NOT NULL,
    lot_code varchar(120) NOT NULL,
    expiry_date date,
    CONSTRAINT uq_inventory_lot UNIQUE (sku_id, lot_code)
);

CREATE TABLE inventory_serial_number (
    id uuid PRIMARY KEY,
    sku_id uuid NOT NULL,
    serial_code varchar(120) NOT NULL,
    CONSTRAINT uq_inventory_serial UNIQUE (sku_id, serial_code)
);

CREATE INDEX ix_inventory_ledger_sku_location ON inventory_stock_ledger_entry (sku_id, warehouse_id, zone_id, bin_id);
CREATE INDEX ix_inventory_balance_sku ON inventory_stock_balance (sku_id);
