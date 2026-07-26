CREATE TABLE logistics_shipment (
    id uuid PRIMARY KEY,
    shipment_number varchar(120) NOT NULL UNIQUE,
    idempotency_key varchar(160) NOT NULL UNIQUE,
    purchase_order_id uuid NOT NULL REFERENCES procurement_purchase_order(id),
    supplier_id uuid NOT NULL REFERENCES procurement_supplier(id),
    company_id uuid NOT NULL REFERENCES company(id),
    branch_id uuid NOT NULL,
    warehouse_id uuid NOT NULL REFERENCES warehouse(id),
    carrier_code varchar(120) NOT NULL,
    origin_port_code varchar(40) NOT NULL,
    destination_port_code varchar(40) NOT NULL,
    incoterm_code varchar(40) NOT NULL,
    estimated_departure date NOT NULL,
    estimated_arrival date NOT NULL,
    status varchar(24) NOT NULL,
    version bigint NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL,
    actor varchar(160) NOT NULL,
    CONSTRAINT fk_logistics_shipment_branch
        FOREIGN KEY (branch_id, company_id) REFERENCES branch(id, company_id),
    CONSTRAINT ck_logistics_shipment_dates
        CHECK (estimated_arrival >= estimated_departure),
    CONSTRAINT ck_logistics_shipment_status CHECK (status IN (
        'DRAFT', 'BOOKED', 'IN_TRANSIT', 'CUSTOMS_HOLD',
        'CUSTOMS_RELEASED', 'DELIVERED', 'CANCELLED'
    )),
    CONSTRAINT ck_logistics_shipment_version CHECK (version >= 0)
);

CREATE TABLE logistics_container (
    id uuid PRIMARY KEY,
    shipment_id uuid NOT NULL REFERENCES logistics_shipment(id),
    container_number varchar(80) NOT NULL UNIQUE,
    container_type varchar(40) NOT NULL,
    gross_weight numeric(19,6) NOT NULL,
    volume_cbm numeric(19,6) NOT NULL,
    loaded_at timestamptz,
    CONSTRAINT ck_logistics_container_measurements
        CHECK (gross_weight > 0 AND volume_cbm > 0)
);

CREATE TABLE logistics_customs_milestone (
    id uuid PRIMARY KEY,
    shipment_id uuid NOT NULL REFERENCES logistics_shipment(id),
    milestone_type varchar(40) NOT NULL,
    reference varchar(160) NOT NULL UNIQUE,
    occurred_at timestamptz NOT NULL,
    notes varchar(1000) NOT NULL DEFAULT '',
    CONSTRAINT ck_logistics_milestone_type CHECK (milestone_type IN (
        'DEPARTED', 'ARRIVED_PORT', 'CUSTOMS_FILED', 'CUSTOMS_HOLD',
        'CUSTOMS_RELEASED', 'INLAND_DELIVERY'
    ))
);

CREATE TABLE logistics_landed_cost_draft (
    id uuid PRIMARY KEY,
    shipment_id uuid NOT NULL REFERENCES logistics_shipment(id),
    idempotency_key varchar(160) NOT NULL UNIQUE,
    currency_code varchar(12) NOT NULL,
    allocation_basis varchar(20) NOT NULL,
    total_amount numeric(19,6) NOT NULL,
    created_at timestamptz NOT NULL,
    actor varchar(160) NOT NULL,
    CONSTRAINT ck_logistics_landed_cost_basis
        CHECK (allocation_basis IN ('VALUE', 'WEIGHT', 'VOLUME', 'QUANTITY')),
    CONSTRAINT ck_logistics_landed_cost_total CHECK (total_amount > 0)
);

CREATE TABLE logistics_landed_cost_component (
    id uuid PRIMARY KEY,
    draft_id uuid NOT NULL REFERENCES logistics_landed_cost_draft(id),
    cost_type varchar(80) NOT NULL,
    amount numeric(19,6) NOT NULL,
    reference varchar(160) NOT NULL,
    CONSTRAINT ck_logistics_cost_component_amount CHECK (amount > 0)
);

CREATE INDEX ix_logistics_shipment_company_status
    ON logistics_shipment(company_id, status, estimated_arrival);
CREATE INDEX ix_logistics_container_shipment ON logistics_container(shipment_id);
CREATE INDEX ix_logistics_milestone_shipment
    ON logistics_customs_milestone(shipment_id, occurred_at);
CREATE INDEX ix_logistics_landed_cost_shipment
    ON logistics_landed_cost_draft(shipment_id);

INSERT INTO iam_permission (id, capability, description)
VALUES
    ('39000000-0000-4000-8000-000000000001', 'logistics.shipment.manage',
     'Create and book company-scoped import shipments'),
    ('39000000-0000-4000-8000-000000000002', 'logistics.shipment.read',
     'Read company-scoped import shipments'),
    ('39000000-0000-4000-8000-000000000003', 'logistics.container.manage',
     'Manage company-scoped import containers'),
    ('39000000-0000-4000-8000-000000000004', 'logistics.customs.manage',
     'Record company-scoped customs milestones'),
    ('39000000-0000-4000-8000-000000000005', 'logistics.landed-cost.manage',
     'Create company-scoped landed-cost drafts')
ON CONFLICT (capability) DO NOTHING;

INSERT INTO platform_domain_event_catalog (event_type, owner_context, description)
VALUES
    ('ShipmentBooked', 'logistics', 'An import shipment was booked'),
    ('ContainerLoaded', 'logistics', 'A container was loaded into an import shipment'),
    ('CustomsReleased', 'logistics', 'Customs release was recorded')
ON CONFLICT (event_type) DO NOTHING;
