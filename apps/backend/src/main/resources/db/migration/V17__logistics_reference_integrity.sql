CREATE TABLE logistics_carrier (
    id uuid PRIMARY KEY,
    code varchar(120) NOT NULL UNIQUE,
    display_name varchar(240) NOT NULL,
    active boolean NOT NULL
);

CREATE TABLE logistics_port (
    id uuid PRIMARY KEY,
    code varchar(40) NOT NULL UNIQUE,
    display_name varchar(240) NOT NULL,
    country_code char(2) NOT NULL,
    active boolean NOT NULL,
    CONSTRAINT ck_logistics_port_country CHECK (country_code ~ '^[A-Z]{2}$')
);

CREATE INDEX ix_logistics_carrier_active_code ON logistics_carrier(active, code);
CREATE INDEX ix_logistics_port_active_code ON logistics_port(active, code);
