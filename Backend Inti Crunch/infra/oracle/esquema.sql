-- CANDIDATO: requiere validación en Oracle 21c. No contiene datos.

-- Ejecutar exclusivamente en un esquema NUEVO dedicado a Inti Crunch.

SET DEFINE OFF;

SET SQLBLANKLINES ON;

WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK;

DECLARE n NUMBER; BEGIN SELECT COUNT(*) INTO n FROM user_tables; IF n <> 0 OR USER IN ('SYS','SYSTEM') THEN RAISE_APPLICATION_ERROR(-20001,'Se requiere un esquema nuevo y vacío de aplicación'); END IF; END;

/

CREATE TABLE cash_movements (
    id NUMBER(19) NOT NULL,
    cash_session_id NUMBER(19) NOT NULL,
    kind VARCHAR2(10 CHAR) NOT NULL,
    amount NUMBER(12,2) NOT NULL,
    reason VARCHAR2(300 CHAR) NOT NULL,
    created_by VARCHAR2(120 CHAR) NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT cash_movements_amount_check CHECK ((amount > (0))),
    CONSTRAINT cash_movements_kind_check CHECK (((kind) IN ('IN', 'OUT')))
);

CREATE TABLE cash_sessions (
    id NUMBER(19) NOT NULL,
    location_id NUMBER(19) NOT NULL,
    opened_by VARCHAR2(120 CHAR) NOT NULL,
    opening_amount NUMBER(12,2) NOT NULL,
    opened_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    closed_by VARCHAR2(120 CHAR),
    closed_at TIMESTAMP(6) WITH TIME ZONE,
    closing_amount NUMBER(12,2),
    status VARCHAR2(20 CHAR) DEFAULT 'OPEN' NOT NULL,
    expected_amount NUMBER(12,2),
    difference NUMBER(12,2),
    employee_id NUMBER(19),
    shift_id NUMBER(19),
    CONSTRAINT cash_sessions_opening_amount_check CHECK ((opening_amount >= (0))),
    CONSTRAINT cash_shift_employee CHECK (((shift_id IS NULL) OR (employee_id IS NOT NULL)))
);

CREATE TABLE courier_zones (
    employee_id NUMBER(19) NOT NULL,
    zone_id NUMBER(19) NOT NULL
);

CREATE TABLE customer_orders (
    id NUMBER(19) NOT NULL,
    customer_name VARCHAR2(120 CHAR) NOT NULL,
    customer_phone VARCHAR2(30 CHAR) NOT NULL,
    channel VARCHAR2(20 CHAR) NOT NULL,
    status VARCHAR2(20 CHAR) NOT NULL,
    total NUMBER(10,2) DEFAULT 0 NOT NULL,
    version NUMBER(19) DEFAULT 0 NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    location_id NUMBER(19) NOT NULL,
    customer_id NUMBER(19),
    delivery_address VARCHAR2(300 CHAR),
    delivery_zone VARCHAR2(100 CHAR),
    customer_subject VARCHAR2(120 CHAR),
    tracking_token VARCHAR2(36 CHAR) DEFAULT LOWER(REGEXP_REPLACE(RAWTOHEX(SYS_GUID()), '(.{8})(.{4})(.{4})(.{4})(.{12})', '\1-\2-\3-\4-\5')) NOT NULL,
    request_key VARCHAR2(36 CHAR),
    subtotal NUMBER(12,2) DEFAULT 0 NOT NULL,
    discount NUMBER(12,2) DEFAULT 0 NOT NULL,
    delivery_fee NUMBER(12,2) DEFAULT 0 NOT NULL,
    notes VARCHAR2(500 CHAR),
    CONSTRAINT customer_orders_delivery_fee_check CHECK ((delivery_fee >= (0))),
    CONSTRAINT customer_orders_discount_check CHECK ((discount >= (0)))
);

CREATE TABLE customers (
    id NUMBER(19) NOT NULL,
    full_name VARCHAR2(150 CHAR) NOT NULL,
    phone VARCHAR2(40 CHAR) NOT NULL,
    email VARCHAR2(150 CHAR),
    created_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    preferences VARCHAR2(500 CHAR),
    subject VARCHAR2(120 CHAR)
);

CREATE TABLE delivery_incidents (
    id NUMBER(19) NOT NULL,
    dispatch_id NUMBER(19) NOT NULL,
    kind VARCHAR2(40 CHAR) NOT NULL,
    description VARCHAR2(500 CHAR) NOT NULL,
    resolution VARCHAR2(500 CHAR),
    created_by VARCHAR2(120 CHAR) NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    resolved_at TIMESTAMP(6) WITH TIME ZONE
);

CREATE TABLE delivery_zones (
    id NUMBER(19) NOT NULL,
    name VARCHAR2(100 CHAR) NOT NULL,
    coverage VARCHAR2(300 CHAR) NOT NULL,
    fee NUMBER(10,2) NOT NULL,
    estimated_minutes NUMBER(10) NOT NULL,
    active NUMBER(1) DEFAULT 1 NOT NULL,
    CONSTRAINT delivery_zones_estimated_minutes_check CHECK ((estimated_minutes > 0)),
    CONSTRAINT delivery_zones_fee_check CHECK ((fee >= (0))),
    CHECK (active IN (0,1))
);

CREATE TABLE dining_tables (
    id NUMBER(19) NOT NULL,
    location_id NUMBER(19) NOT NULL,
    table_code VARCHAR2(20 CHAR) NOT NULL,
    capacity NUMBER(10) NOT NULL,
    active NUMBER(1) DEFAULT 1 NOT NULL,
    CONSTRAINT dining_tables_capacity_check CHECK ((capacity > 0)),
    CHECK (active IN (0,1))
);

CREATE TABLE dispatch_assignments (
    id NUMBER(19) NOT NULL,
    dispatch_id NUMBER(19) NOT NULL,
    employee_id NUMBER(19) NOT NULL,
    assigned_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ended_at TIMESTAMP(6) WITH TIME ZONE
);

CREATE TABLE dispatch_events (
    id NUMBER(19) NOT NULL,
    dispatch_id NUMBER(19) NOT NULL,
    status VARCHAR2(20 CHAR) NOT NULL,
    note VARCHAR2(300 CHAR),
    created_by VARCHAR2(120 CHAR) NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE dispatch_zones (
    dispatch_id NUMBER(19) NOT NULL,
    zone_id NUMBER(19) NOT NULL
);

CREATE TABLE dispatches (
    id NUMBER(19) NOT NULL,
    order_id NUMBER(19) NOT NULL,
    location_id NUMBER(19) NOT NULL,
    delivery_type VARCHAR2(20 CHAR) NOT NULL,
    address VARCHAR2(300 CHAR),
    zone VARCHAR2(100 CHAR),
    status VARCHAR2(20 CHAR) DEFAULT 'QUEUED' NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    delivered_at TIMESTAMP(6) WITH TIME ZONE,
    customer_note VARCHAR2(300 CHAR),
    estimated_at TIMESTAMP(6) WITH TIME ZONE
);

CREATE TABLE employee_locations (
    employee_id NUMBER(19) NOT NULL,
    location_id NUMBER(19) NOT NULL
);

CREATE TABLE employee_roles (
    employee_id NUMBER(19) NOT NULL,
    role_name VARCHAR2(40 CHAR) NOT NULL,
    assigned_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE employee_shifts (
    shift_id NUMBER(19) NOT NULL,
    employee_id NUMBER(19) NOT NULL,
    role_name VARCHAR2(40 CHAR) NOT NULL,
    check_in TIMESTAMP(6) WITH TIME ZONE,
    check_out TIMESTAMP(6) WITH TIME ZONE,
    incident VARCHAR2(300 CHAR),
    attendance_status VARCHAR2(20 CHAR) DEFAULT 'PENDING' NOT NULL,
    late_minutes NUMBER(10) DEFAULT 0 NOT NULL
);

CREATE TABLE employees (
    id NUMBER(19) NOT NULL,
    keycloak_subject VARCHAR2(100 CHAR),
    full_name VARCHAR2(150 CHAR) NOT NULL,
    phone VARCHAR2(40 CHAR),
    active NUMBER(1) DEFAULT 1 NOT NULL,
    hired_on date DEFAULT CURRENT_DATE NOT NULL,
    available NUMBER(1) DEFAULT 1 NOT NULL,
    CHECK (active IN (0,1)),
    CHECK (available IN (0,1))
);

CREATE TABLE ingredients (
    id NUMBER(19) NOT NULL,
    sku VARCHAR2(40 CHAR) NOT NULL,
    name VARCHAR2(120 CHAR) NOT NULL,
    unit VARCHAR2(20 CHAR) NOT NULL,
    minimum_stock NUMBER(12,3) DEFAULT 0 NOT NULL,
    active NUMBER(1) DEFAULT 1 NOT NULL,
    version NUMBER(19) DEFAULT 0 NOT NULL,
    category VARCHAR2(80 CHAR) DEFAULT 'General' NOT NULL,
    CONSTRAINT ingredients_minimum_stock_check CHECK ((minimum_stock >= (0))),
    CHECK (active IN (0,1))
);

CREATE TABLE inventory_balances (
    id NUMBER(19) NOT NULL,
    location_id NUMBER(19) NOT NULL,
    ingredient_id NUMBER(19) NOT NULL,
    quantity NUMBER(12,3) DEFAULT 0 NOT NULL,
    version NUMBER(19) DEFAULT 0 NOT NULL,
    CONSTRAINT inventory_balances_quantity_check CHECK ((quantity >= (0)))
);

CREATE TABLE inventory_lots (
    id NUMBER(19) NOT NULL,
    warehouse_id NUMBER(19) NOT NULL,
    ingredient_id NUMBER(19) NOT NULL,
    batch_code VARCHAR2(80 CHAR) NOT NULL,
    expires_on date,
    quantity NUMBER(12,3) NOT NULL,
    CONSTRAINT inventory_lots_quantity_check CHECK ((quantity >= (0)))
);

CREATE TABLE inventory_movement_lines (
    id NUMBER(19) NOT NULL,
    movement_id NUMBER(19) NOT NULL,
    ingredient_id NUMBER(19) NOT NULL,
    quantity NUMBER(12,3) NOT NULL,
    unit_cost NUMBER(12,2),
    CONSTRAINT inventory_movement_lines_quantity_check CHECK ((quantity > (0))),
    CONSTRAINT inventory_movement_lines_unit_cost_check CHECK ((unit_cost >= (0)))
);

CREATE TABLE inventory_movements (
    id NUMBER(19) NOT NULL,
    location_id NUMBER(19) NOT NULL,
    kind VARCHAR2(20 CHAR) NOT NULL,
    reference VARCHAR2(100 CHAR),
    note VARCHAR2(500 CHAR),
    created_by VARCHAR2(120 CHAR) NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    warehouse_id NUMBER(19) DEFAULT 1,
    target_warehouse_id NUMBER(19)
);

CREATE TABLE kitchen_ticket_lines (
    ticket_id NUMBER(19) NOT NULL,
    order_line_id NUMBER(19) NOT NULL,
    station VARCHAR2(40 CHAR) DEFAULT 'PARRILLA' NOT NULL,
    status VARCHAR2(20 CHAR) DEFAULT 'PENDING' NOT NULL
);

CREATE TABLE kitchen_tickets (
    id NUMBER(19) NOT NULL,
    order_id NUMBER(19) NOT NULL,
    status VARCHAR2(20 CHAR) DEFAULT 'RECEIVED' NOT NULL,
    received_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ready_at TIMESTAMP(6) WITH TIME ZONE
);

CREATE TABLE locations (
    id NUMBER(19) NOT NULL,
    name VARCHAR2(100 CHAR) NOT NULL,
    address VARCHAR2(250 CHAR),
    active NUMBER(1) DEFAULT 1 NOT NULL,
    CHECK (active IN (0,1))
);

CREATE TABLE loyalty_movements (
    id NUMBER(19) NOT NULL,
    customer_id NUMBER(19) NOT NULL,
    order_id NUMBER(19),
    points NUMBER(10) NOT NULL,
    reason VARCHAR2(200 CHAR) NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE order_discounts (
    id NUMBER(19) NOT NULL,
    order_id NUMBER(19) NOT NULL,
    amount NUMBER(12,2) NOT NULL,
    reason VARCHAR2(300 CHAR) NOT NULL,
    authorized_by VARCHAR2(120 CHAR) NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT order_discounts_amount_check CHECK ((amount > (0)))
);

CREATE TABLE order_inventory_consumptions (
    order_line_id NUMBER(19) NOT NULL,
    ingredient_id NUMBER(19) NOT NULL,
    consumed_quantity NUMBER(12,3) NOT NULL,
    consumed_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT order_inventory_consumptions_consumed_quantity_check CHECK ((consumed_quantity > (0)))
);

CREATE TABLE order_line_options (
    order_line_id NUMBER(19) NOT NULL,
    option_id NUMBER(19) NOT NULL,
    option_name VARCHAR2(100 CHAR) NOT NULL,
    extra_price NUMBER(10,2) NOT NULL
);

CREATE TABLE order_lines (
    id NUMBER(19) NOT NULL,
    order_id NUMBER(19) NOT NULL,
    product_id NUMBER(19) NOT NULL,
    quantity NUMBER(10) NOT NULL,
    unit_price NUMBER(10,2) NOT NULL,
    product_name VARCHAR2(120 CHAR),
    notes VARCHAR2(300 CHAR),
    CONSTRAINT order_lines_quantity_check CHECK ((quantity > 0)),
    CONSTRAINT order_lines_unit_price_check CHECK ((unit_price > (0)))
);

CREATE TABLE order_prepared_consumptions (
    order_line_id NUMBER(19) NOT NULL,
    quantity NUMBER(10) NOT NULL,
    CONSTRAINT order_prepared_consumptions_quantity_check CHECK ((quantity > 0))
);

CREATE TABLE order_status_events (
    id NUMBER(19) NOT NULL,
    order_id NUMBER(19) NOT NULL,
    old_status VARCHAR2(20 CHAR),
    new_status VARCHAR2(20 CHAR) NOT NULL,
    changed_by VARCHAR2(120 CHAR) NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE payment_allocations (
    payment_id NUMBER(19) NOT NULL,
    order_id NUMBER(19) NOT NULL,
    amount NUMBER(12,2) NOT NULL,
    CONSTRAINT payment_allocations_amount_check CHECK ((amount > (0)))
);

CREATE TABLE payment_document_files (
    document_id NUMBER(19) NOT NULL,
    content BLOB NOT NULL
);

CREATE TABLE payment_document_lines (
    id NUMBER(19) NOT NULL,
    document_id NUMBER(19) NOT NULL,
    product_name CLOB NOT NULL,
    quantity NUMBER(10) NOT NULL,
    unit_price NUMBER(12,2) NOT NULL,
    line_total NUMBER(12,2) NOT NULL
);

CREATE TABLE payment_documents (
    id NUMBER(19) NOT NULL,
    payment_id NUMBER(19) NOT NULL,
    order_id NUMBER(19) NOT NULL,
    document_type VARCHAR2(12 CHAR) NOT NULL,
    series VARCHAR2(8 CHAR) NOT NULL,
    "NUMBER" NUMBER(19) NOT NULL,
    customer_document_type VARCHAR2(12 CHAR),
    customer_document_number VARCHAR2(20 CHAR),
    customer_address VARCHAR2(300 CHAR),
    seller_name VARCHAR2(150 CHAR) DEFAULT 'Inti Crunch · La Rinconada' NOT NULL,
    seller_tax_id VARCHAR2(20 CHAR),
    subtotal NUMBER(12,2) NOT NULL,
    tax_rate NUMBER(5,2) DEFAULT 18.00 NOT NULL,
    tax_amount NUMBER(12,2) NOT NULL,
    total NUMBER(12,2) NOT NULL,
    issued_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    seller_address VARCHAR2(300 CHAR),
    seller_phone VARCHAR2(40 CHAR),
    customer_name VARCHAR2(150 CHAR),
    customer_phone VARCHAR2(40 CHAR),
    cashier VARCHAR2(150 CHAR),
    order_total NUMBER(12,2),
    pending_amount NUMBER(12,2) DEFAULT 0 NOT NULL,
    discount NUMBER(12,2) DEFAULT 0 NOT NULL,
    delivery_fee NUMBER(12,2) DEFAULT 0 NOT NULL,
    payment_summary VARCHAR2(500 CHAR),
    CONSTRAINT payment_documents_document_type_check CHECK (((document_type) IN ('BOLETA', 'FACTURA')))
);

CREATE TABLE payment_methods (
    id NUMBER(19) NOT NULL,
    payment_id NUMBER(19) NOT NULL,
    method VARCHAR2(20 CHAR) NOT NULL,
    amount NUMBER(12,2) NOT NULL,
    tip NUMBER(12,2) DEFAULT 0 NOT NULL,
    reference VARCHAR2(120 CHAR),
    CONSTRAINT payment_methods_amount_check CHECK ((amount > (0))),
    CONSTRAINT payment_methods_tip_check CHECK ((tip >= (0)))
);

CREATE TABLE payment_refunds (
    id NUMBER(19) NOT NULL,
    payment_id NUMBER(19) NOT NULL,
    order_id NUMBER(19) NOT NULL,
    cash_session_id NUMBER(19) NOT NULL,
    method VARCHAR2(20 CHAR) NOT NULL,
    amount NUMBER(12,2) NOT NULL,
    reason VARCHAR2(300 CHAR) NOT NULL,
    created_by VARCHAR2(120 CHAR) NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT payment_refunds_amount_check CHECK ((amount > (0)))
);

CREATE TABLE payments (
    id NUMBER(19) NOT NULL,
    cash_session_id NUMBER(19),
    method VARCHAR2(20 CHAR) NOT NULL,
    amount NUMBER(12,2) NOT NULL,
    status VARCHAR2(20 CHAR) DEFAULT 'CONFIRMED' NOT NULL,
    reference VARCHAR2(120 CHAR),
    created_by VARCHAR2(120 CHAR) NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    request_key VARCHAR2(36 CHAR),
    tip NUMBER(12,2) DEFAULT 0 NOT NULL,
    CONSTRAINT payments_amount_check CHECK ((amount > (0))),
    CONSTRAINT payments_tip_check CHECK ((tip >= (0)))
);

CREATE TABLE prepared_stock (
    product_id NUMBER(19) NOT NULL,
    quantity NUMBER(10) DEFAULT 0 NOT NULL,
    CONSTRAINT prepared_stock_quantity_check CHECK ((quantity >= 0))
);

CREATE TABLE product_option_links (
    product_id NUMBER(19) NOT NULL,
    option_id NUMBER(19) NOT NULL
);

CREATE TABLE product_options (
    id NUMBER(19) NOT NULL,
    name VARCHAR2(100 CHAR) NOT NULL,
    extra_price NUMBER(10,2) DEFAULT 0 NOT NULL,
    active NUMBER(1) DEFAULT 1 NOT NULL,
    CONSTRAINT product_options_extra_price_check CHECK ((extra_price >= (0))),
    CHECK (active IN (0,1))
);

CREATE TABLE production_run_lines (
    production_run_id NUMBER(19) NOT NULL,
    ingredient_id NUMBER(19) NOT NULL,
    consumed_quantity NUMBER(12,3) NOT NULL,
    CONSTRAINT production_run_lines_consumed_quantity_check CHECK ((consumed_quantity > (0)))
);

CREATE TABLE production_runs (
    id NUMBER(19) NOT NULL,
    location_id NUMBER(19) NOT NULL,
    recipe_id NUMBER(19) NOT NULL,
    planned_servings NUMBER(10) NOT NULL,
    produced_servings NUMBER(10) DEFAULT 0 NOT NULL,
    status VARCHAR2(20 CHAR) DEFAULT 'COMPLETED' NOT NULL,
    note VARCHAR2(500 CHAR),
    created_by VARCHAR2(120 CHAR) NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT production_runs_planned_servings_check CHECK ((planned_servings > 0)),
    CONSTRAINT production_runs_produced_servings_check CHECK ((produced_servings >= 0)),
    CONSTRAINT production_runs_status_check CHECK (((status) IN ('COMPLETED', 'CANCELLED')))
);

CREATE TABLE products (
    id NUMBER(19) NOT NULL,
    name VARCHAR2(120 CHAR) NOT NULL,
    description VARCHAR2(500 CHAR) NOT NULL,
    price NUMBER(10,2) NOT NULL,
    category VARCHAR2(40 CHAR) NOT NULL,
    image_url VARCHAR2(500 CHAR),
    active NUMBER(1) DEFAULT 1 NOT NULL,
    available NUMBER(1) DEFAULT 1 NOT NULL,
    version NUMBER(19) DEFAULT 0 NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT products_price_check CHECK ((price > (0))),
    CHECK (active IN (0,1)),
    CHECK (available IN (0,1))
);

CREATE TABLE promotion_products (
    promotion_id NUMBER(19) NOT NULL,
    product_id NUMBER(19) NOT NULL
);

CREATE TABLE promotion_redemptions (
    promotion_id NUMBER(19) NOT NULL,
    customer_id NUMBER(19) NOT NULL,
    order_id NUMBER(19) NOT NULL,
    redeemed_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    discount NUMBER(12,2) DEFAULT 0 NOT NULL
);

CREATE TABLE promotions (
    id NUMBER(19) NOT NULL,
    name VARCHAR2(150 CHAR) NOT NULL,
    code VARCHAR2(40 CHAR),
    discount_percent NUMBER(5,2),
    discount_amount NUMBER(10,2),
    starts_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    ends_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    active NUMBER(1) DEFAULT 1 NOT NULL,
    image_url CLOB,
    minimum_order NUMBER(12,2) DEFAULT 0 NOT NULL,
    maximum_uses NUMBER(10),
    CONSTRAINT promotions_check CHECK (((discount_percent IS NULL AND discount_amount IS NOT NULL) OR (discount_percent IS NOT NULL AND discount_amount IS NULL))),
    CONSTRAINT promotions_maximum_uses_check CHECK ((maximum_uses > 0)),
    CONSTRAINT promotions_minimum_order_check CHECK ((minimum_order >= (0))),
    CHECK (active IN (0,1))
);

CREATE TABLE purchase_invoice_payments (
    id NUMBER(19) NOT NULL,
    invoice_id NUMBER(19) NOT NULL,
    amount NUMBER(12,2) NOT NULL,
    reference VARCHAR2(120 CHAR),
    created_by VARCHAR2(120 CHAR) NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT purchase_invoice_payments_amount_check CHECK ((amount > (0)))
);

CREATE TABLE purchase_invoices (
    id NUMBER(19) NOT NULL,
    purchase_order_id NUMBER(19) NOT NULL,
    supplier_id NUMBER(19) NOT NULL,
    "NUMBER" VARCHAR2(80 CHAR) NOT NULL,
    total NUMBER(12,2) NOT NULL,
    due_date date NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT purchase_invoices_total_check CHECK ((total > (0)))
);

CREATE TABLE purchase_order_lines (
    id NUMBER(19) NOT NULL,
    purchase_order_id NUMBER(19) NOT NULL,
    ingredient_id NUMBER(19) NOT NULL,
    quantity NUMBER(12,3) NOT NULL,
    unit_price NUMBER(12,2) NOT NULL,
    received_quantity NUMBER(12,3) DEFAULT 0 NOT NULL,
    CONSTRAINT purchase_order_lines_quantity_check CHECK ((quantity > (0))),
    CONSTRAINT purchase_order_lines_unit_price_check CHECK ((unit_price >= (0)))
);

CREATE TABLE purchase_orders (
    id NUMBER(19) NOT NULL,
    location_id NUMBER(19) NOT NULL,
    supplier_id NUMBER(19) NOT NULL,
    status VARCHAR2(20 CHAR) DEFAULT 'PENDING' NOT NULL,
    total NUMBER(12,2) DEFAULT 0 NOT NULL,
    created_by VARCHAR2(120 CHAR) NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    received_at TIMESTAMP(6) WITH TIME ZONE,
    request_id NUMBER(19)
);

CREATE TABLE purchase_receipt_lines (
    receipt_id NUMBER(19) NOT NULL,
    purchase_line_id NUMBER(19) NOT NULL,
    quantity NUMBER(12,3) NOT NULL,
    observed_quantity NUMBER(12,3) DEFAULT 0 NOT NULL,
    note VARCHAR2(300 CHAR),
    CONSTRAINT purchase_receipt_lines_observed_quantity_check CHECK ((observed_quantity >= (0))),
    CONSTRAINT purchase_receipt_lines_quantity_check CHECK (((quantity >= (0)) AND ((quantity + observed_quantity) > (0))))
);

CREATE TABLE purchase_receipts (
    id NUMBER(19) NOT NULL,
    purchase_order_id NUMBER(19) NOT NULL,
    document_number VARCHAR2(80 CHAR),
    warehouse_id NUMBER(19) NOT NULL,
    received_by VARCHAR2(120 CHAR) NOT NULL,
    received_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    note VARCHAR2(500 CHAR)
);

CREATE TABLE purchase_request_lines (
    request_id NUMBER(19) NOT NULL,
    ingredient_id NUMBER(19) NOT NULL,
    quantity NUMBER(12,3) NOT NULL,
    CONSTRAINT purchase_request_lines_quantity_check CHECK ((quantity > (0)))
);

CREATE TABLE purchase_requests (
    id NUMBER(19) NOT NULL,
    note VARCHAR2(500 CHAR),
    status VARCHAR2(20 CHAR) DEFAULT 'PENDING' NOT NULL,
    created_by VARCHAR2(120 CHAR) NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE recipe_ingredients (
    recipe_id NUMBER(19) NOT NULL,
    ingredient_id NUMBER(19) NOT NULL,
    quantity NUMBER(12,3) NOT NULL,
    CONSTRAINT recipe_ingredients_quantity_check CHECK ((quantity > (0)))
);

CREATE TABLE recipes (
    id NUMBER(19) NOT NULL,
    product_id NUMBER(19) NOT NULL,
    version NUMBER(10) DEFAULT 1 NOT NULL,
    servings NUMBER(10) DEFAULT 1 NOT NULL,
    active NUMBER(1) DEFAULT 1 NOT NULL,
    CONSTRAINT recipes_servings_check CHECK ((servings > 0)),
    CHECK (active IN (0,1))
);

CREATE TABLE reservation_events (
    id NUMBER(19) NOT NULL,
    reservation_id NUMBER(19) NOT NULL,
    status VARCHAR2(20 CHAR) NOT NULL,
    note VARCHAR2(300 CHAR),
    created_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE reservation_tables (
    reservation_id NUMBER(19) NOT NULL,
    table_id NUMBER(19) NOT NULL
);

CREATE TABLE reservations (
    id NUMBER(19) NOT NULL,
    customer_id NUMBER(19),
    guest_name VARCHAR2(150 CHAR) NOT NULL,
    guest_phone VARCHAR2(40 CHAR) NOT NULL,
    party_size NUMBER(10) NOT NULL,
    starts_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    status VARCHAR2(20 CHAR) DEFAULT 'BOOKED' NOT NULL,
    notes VARCHAR2(500 CHAR),
    created_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ends_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT reservations_party_size_check CHECK ((party_size > 0)),
    CONSTRAINT valid_reservation_period CHECK ((ends_at > starts_at))
);

CREATE TABLE supplier_ingredients (
    supplier_id NUMBER(19) NOT NULL,
    ingredient_id NUMBER(19) NOT NULL,
    supplier_sku VARCHAR2(80 CHAR),
    last_price NUMBER(12,2),
    updated_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT supplier_ingredients_last_price_check CHECK ((last_price >= (0)))
);

CREATE TABLE suppliers (
    id NUMBER(19) NOT NULL,
    name VARCHAR2(150 CHAR) NOT NULL,
    tax_id VARCHAR2(20 CHAR),
    phone VARCHAR2(40 CHAR),
    email VARCHAR2(150 CHAR),
    active NUMBER(1) DEFAULT 1 NOT NULL,
    CHECK (active IN (0,1))
);

CREATE TABLE visit_orders (
    visit_id NUMBER(19) NOT NULL,
    order_id NUMBER(19) NOT NULL
);

CREATE TABLE visit_tables (
    visit_id NUMBER(19) NOT NULL,
    table_id NUMBER(19) NOT NULL
);

CREATE TABLE visits (
    id NUMBER(19) NOT NULL,
    reservation_id NUMBER(19),
    guest_name VARCHAR2(150 CHAR) NOT NULL,
    party_size NUMBER(10) NOT NULL,
    arrived_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    left_at TIMESTAMP(6) WITH TIME ZONE,
    CONSTRAINT visits_party_size_check CHECK ((party_size > 0))
);

CREATE TABLE warehouse_balances (
    warehouse_id NUMBER(19) NOT NULL,
    ingredient_id NUMBER(19) NOT NULL,
    quantity NUMBER(12,3) DEFAULT 0 NOT NULL,
    CONSTRAINT warehouse_balances_quantity_check CHECK ((quantity >= (0)))
);

CREATE TABLE warehouses (
    id NUMBER(19) NOT NULL,
    location_id NUMBER(19) NOT NULL,
    name VARCHAR2(100 CHAR) NOT NULL,
    active NUMBER(1) DEFAULT 1 NOT NULL,
    CHECK (active IN (0,1))
);

CREATE TABLE work_shifts (
    id NUMBER(19) NOT NULL,
    location_id NUMBER(19) NOT NULL,
    name VARCHAR2(100 CHAR) NOT NULL,
    starts_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    ends_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    status VARCHAR2(20 CHAR) DEFAULT 'PLANNED' NOT NULL,
    CONSTRAINT work_shifts_check CHECK ((ends_at > starts_at))
);

CREATE SEQUENCE cash_movements_id_seq START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE SEQUENCE cash_sessions_id_seq START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE SEQUENCE customer_orders_id_seq START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE SEQUENCE customers_id_seq START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE SEQUENCE delivery_incidents_id_seq START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE SEQUENCE delivery_zones_id_seq START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE SEQUENCE dining_tables_id_seq START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE SEQUENCE dispatch_assignments_id_seq START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE SEQUENCE dispatch_events_id_seq START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE SEQUENCE dispatches_id_seq START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE SEQUENCE employees_id_seq START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE SEQUENCE ingredients_id_seq START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE SEQUENCE inventory_balances_id_seq START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE SEQUENCE inventory_lots_id_seq START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE SEQUENCE inventory_movement_lines_id_seq START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE SEQUENCE inventory_movements_id_seq START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE SEQUENCE kitchen_tickets_id_seq START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE SEQUENCE locations_id_seq START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE SEQUENCE loyalty_movements_id_seq START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE SEQUENCE order_discounts_id_seq START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE SEQUENCE order_lines_id_seq START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE SEQUENCE order_status_events_id_seq START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE SEQUENCE payment_document_lines_id_seq START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE SEQUENCE payment_document_number_seq START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE SEQUENCE payment_documents_id_seq START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE SEQUENCE payment_methods_id_seq START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE SEQUENCE payment_refunds_id_seq START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE SEQUENCE payments_id_seq START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE SEQUENCE product_options_id_seq START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE SEQUENCE production_runs_id_seq START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE SEQUENCE products_id_seq START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE SEQUENCE promotions_id_seq START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE SEQUENCE purchase_invoice_payments_id_seq START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE SEQUENCE purchase_invoices_id_seq START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE SEQUENCE purchase_order_lines_id_seq START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE SEQUENCE purchase_orders_id_seq START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE SEQUENCE purchase_receipts_id_seq START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE SEQUENCE purchase_requests_id_seq START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE SEQUENCE recipes_id_seq START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE SEQUENCE reservation_events_id_seq START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE SEQUENCE reservations_id_seq START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE SEQUENCE suppliers_id_seq START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE SEQUENCE visits_id_seq START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE SEQUENCE warehouses_id_seq START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE SEQUENCE work_shifts_id_seq START WITH 1 INCREMENT BY 1 NOCACHE;

ALTER TABLE cash_movements MODIFY (id DEFAULT cash_movements_id_seq.NEXTVAL);

ALTER TABLE cash_sessions MODIFY (id DEFAULT cash_sessions_id_seq.NEXTVAL);

ALTER TABLE customer_orders MODIFY (id DEFAULT customer_orders_id_seq.NEXTVAL);

ALTER TABLE customers MODIFY (id DEFAULT customers_id_seq.NEXTVAL);

ALTER TABLE delivery_incidents MODIFY (id DEFAULT delivery_incidents_id_seq.NEXTVAL);

ALTER TABLE delivery_zones MODIFY (id DEFAULT delivery_zones_id_seq.NEXTVAL);

ALTER TABLE dining_tables MODIFY (id DEFAULT dining_tables_id_seq.NEXTVAL);

ALTER TABLE dispatch_assignments MODIFY (id DEFAULT dispatch_assignments_id_seq.NEXTVAL);

ALTER TABLE dispatch_events MODIFY (id DEFAULT dispatch_events_id_seq.NEXTVAL);

ALTER TABLE dispatches MODIFY (id DEFAULT dispatches_id_seq.NEXTVAL);

ALTER TABLE employees MODIFY (id DEFAULT employees_id_seq.NEXTVAL);

ALTER TABLE ingredients MODIFY (id DEFAULT ingredients_id_seq.NEXTVAL);

ALTER TABLE inventory_balances MODIFY (id DEFAULT inventory_balances_id_seq.NEXTVAL);

ALTER TABLE inventory_lots MODIFY (id DEFAULT inventory_lots_id_seq.NEXTVAL);

ALTER TABLE inventory_movement_lines MODIFY (id DEFAULT inventory_movement_lines_id_seq.NEXTVAL);

ALTER TABLE inventory_movements MODIFY (id DEFAULT inventory_movements_id_seq.NEXTVAL);

ALTER TABLE kitchen_tickets MODIFY (id DEFAULT kitchen_tickets_id_seq.NEXTVAL);

ALTER TABLE locations MODIFY (id DEFAULT locations_id_seq.NEXTVAL);

ALTER TABLE loyalty_movements MODIFY (id DEFAULT loyalty_movements_id_seq.NEXTVAL);

ALTER TABLE order_discounts MODIFY (id DEFAULT order_discounts_id_seq.NEXTVAL);

ALTER TABLE order_lines MODIFY (id DEFAULT order_lines_id_seq.NEXTVAL);

ALTER TABLE order_status_events MODIFY (id DEFAULT order_status_events_id_seq.NEXTVAL);

ALTER TABLE payment_document_lines MODIFY (id DEFAULT payment_document_lines_id_seq.NEXTVAL);

ALTER TABLE payment_documents MODIFY (id DEFAULT payment_documents_id_seq.NEXTVAL);

ALTER TABLE payment_methods MODIFY (id DEFAULT payment_methods_id_seq.NEXTVAL);

ALTER TABLE payment_refunds MODIFY (id DEFAULT payment_refunds_id_seq.NEXTVAL);

ALTER TABLE payments MODIFY (id DEFAULT payments_id_seq.NEXTVAL);

ALTER TABLE product_options MODIFY (id DEFAULT product_options_id_seq.NEXTVAL);

ALTER TABLE production_runs MODIFY (id DEFAULT production_runs_id_seq.NEXTVAL);

ALTER TABLE products MODIFY (id DEFAULT products_id_seq.NEXTVAL);

ALTER TABLE promotions MODIFY (id DEFAULT promotions_id_seq.NEXTVAL);

ALTER TABLE purchase_invoice_payments MODIFY (id DEFAULT purchase_invoice_payments_id_seq.NEXTVAL);

ALTER TABLE purchase_invoices MODIFY (id DEFAULT purchase_invoices_id_seq.NEXTVAL);

ALTER TABLE purchase_order_lines MODIFY (id DEFAULT purchase_order_lines_id_seq.NEXTVAL);

ALTER TABLE purchase_orders MODIFY (id DEFAULT purchase_orders_id_seq.NEXTVAL);

ALTER TABLE purchase_receipts MODIFY (id DEFAULT purchase_receipts_id_seq.NEXTVAL);

ALTER TABLE purchase_requests MODIFY (id DEFAULT purchase_requests_id_seq.NEXTVAL);

ALTER TABLE recipes MODIFY (id DEFAULT recipes_id_seq.NEXTVAL);

ALTER TABLE reservation_events MODIFY (id DEFAULT reservation_events_id_seq.NEXTVAL);

ALTER TABLE reservations MODIFY (id DEFAULT reservations_id_seq.NEXTVAL);

ALTER TABLE suppliers MODIFY (id DEFAULT suppliers_id_seq.NEXTVAL);

ALTER TABLE visits MODIFY (id DEFAULT visits_id_seq.NEXTVAL);

ALTER TABLE warehouses MODIFY (id DEFAULT warehouses_id_seq.NEXTVAL);

ALTER TABLE work_shifts MODIFY (id DEFAULT work_shifts_id_seq.NEXTVAL);

ALTER TABLE cash_movements ADD CONSTRAINT cash_movements_pkey PRIMARY KEY (id);

ALTER TABLE cash_sessions ADD CONSTRAINT cash_sessions_pkey PRIMARY KEY (id);

ALTER TABLE courier_zones ADD CONSTRAINT courier_zones_pkey PRIMARY KEY (employee_id, zone_id);

ALTER TABLE customer_orders ADD CONSTRAINT customer_orders_pkey PRIMARY KEY (id);

ALTER TABLE customers ADD CONSTRAINT customers_phone_key UNIQUE (phone);

ALTER TABLE customers ADD CONSTRAINT customers_pkey PRIMARY KEY (id);

ALTER TABLE delivery_incidents ADD CONSTRAINT delivery_incidents_pkey PRIMARY KEY (id);

ALTER TABLE delivery_zones ADD CONSTRAINT delivery_zones_name_key UNIQUE (name);

ALTER TABLE delivery_zones ADD CONSTRAINT delivery_zones_pkey PRIMARY KEY (id);

ALTER TABLE dining_tables ADD CONSTRAINT dining_tables_location_id_table_code_key UNIQUE (location_id, table_code);

ALTER TABLE dining_tables ADD CONSTRAINT dining_tables_pkey PRIMARY KEY (id);

ALTER TABLE dispatch_assignments ADD CONSTRAINT dispatch_assignments_pkey PRIMARY KEY (id);

ALTER TABLE dispatch_events ADD CONSTRAINT dispatch_events_pkey PRIMARY KEY (id);

ALTER TABLE dispatch_zones ADD CONSTRAINT dispatch_zones_pkey PRIMARY KEY (dispatch_id, zone_id);

ALTER TABLE dispatches ADD CONSTRAINT dispatches_order_id_key UNIQUE (order_id);

ALTER TABLE dispatches ADD CONSTRAINT dispatches_pkey PRIMARY KEY (id);

ALTER TABLE employee_locations ADD CONSTRAINT employee_locations_pkey PRIMARY KEY (employee_id, location_id);

ALTER TABLE employee_roles ADD CONSTRAINT employee_roles_pkey PRIMARY KEY (employee_id, role_name);

ALTER TABLE employee_shifts ADD CONSTRAINT employee_shifts_pkey PRIMARY KEY (shift_id, employee_id);

ALTER TABLE employees ADD CONSTRAINT employees_keycloak_subject_key UNIQUE (keycloak_subject);

ALTER TABLE employees ADD CONSTRAINT employees_pkey PRIMARY KEY (id);

ALTER TABLE ingredients ADD CONSTRAINT ingredients_name_key UNIQUE (name);

ALTER TABLE ingredients ADD CONSTRAINT ingredients_pkey PRIMARY KEY (id);

ALTER TABLE ingredients ADD CONSTRAINT ingredients_sku_key UNIQUE (sku);

ALTER TABLE inventory_balances ADD CONSTRAINT inventory_balances_location_id_ingredient_id_key UNIQUE (location_id, ingredient_id);

ALTER TABLE inventory_balances ADD CONSTRAINT inventory_balances_pkey PRIMARY KEY (id);

ALTER TABLE inventory_lots ADD CONSTRAINT inventory_lots_pkey PRIMARY KEY (id);

ALTER TABLE inventory_lots ADD CONSTRAINT inventory_lots_warehouse_id_ingredient_id_batch_code_key UNIQUE (warehouse_id, ingredient_id, batch_code);

ALTER TABLE inventory_movement_lines ADD CONSTRAINT inventory_movement_lines_pkey PRIMARY KEY (id);

ALTER TABLE inventory_movements ADD CONSTRAINT inventory_movements_pkey PRIMARY KEY (id);

ALTER TABLE kitchen_ticket_lines ADD CONSTRAINT kitchen_ticket_lines_pkey PRIMARY KEY (ticket_id, order_line_id, station);

ALTER TABLE kitchen_tickets ADD CONSTRAINT kitchen_tickets_order_id_key UNIQUE (order_id);

ALTER TABLE kitchen_tickets ADD CONSTRAINT kitchen_tickets_pkey PRIMARY KEY (id);

ALTER TABLE locations ADD CONSTRAINT locations_name_key UNIQUE (name);

ALTER TABLE locations ADD CONSTRAINT locations_pkey PRIMARY KEY (id);

ALTER TABLE loyalty_movements ADD CONSTRAINT loyalty_movements_pkey PRIMARY KEY (id);

ALTER TABLE order_discounts ADD CONSTRAINT order_discounts_pkey PRIMARY KEY (id);

ALTER TABLE order_inventory_consumptions ADD CONSTRAINT order_inventory_consumptions_pkey PRIMARY KEY (order_line_id, ingredient_id);

ALTER TABLE order_line_options ADD CONSTRAINT order_line_options_pkey PRIMARY KEY (order_line_id, option_id);

ALTER TABLE order_lines ADD CONSTRAINT order_lines_pkey PRIMARY KEY (id);

ALTER TABLE order_prepared_consumptions ADD CONSTRAINT order_prepared_consumptions_pkey PRIMARY KEY (order_line_id);

ALTER TABLE order_status_events ADD CONSTRAINT order_status_events_pkey PRIMARY KEY (id);

ALTER TABLE payment_allocations ADD CONSTRAINT payment_allocations_pkey PRIMARY KEY (payment_id, order_id);

ALTER TABLE payment_document_files ADD CONSTRAINT payment_document_files_pkey PRIMARY KEY (document_id);

ALTER TABLE payment_document_lines ADD CONSTRAINT payment_document_lines_pkey PRIMARY KEY (id);

ALTER TABLE payment_documents ADD CONSTRAINT payment_documents_payment_id_order_id_key UNIQUE (payment_id, order_id);

ALTER TABLE payment_documents ADD CONSTRAINT payment_documents_pkey PRIMARY KEY (id);

ALTER TABLE payment_documents ADD CONSTRAINT payment_documents_series_number_key UNIQUE (series, "NUMBER");

ALTER TABLE payment_methods ADD CONSTRAINT payment_methods_pkey PRIMARY KEY (id);

ALTER TABLE payment_refunds ADD CONSTRAINT payment_refunds_pkey PRIMARY KEY (id);

ALTER TABLE payments ADD CONSTRAINT payments_pkey PRIMARY KEY (id);

ALTER TABLE prepared_stock ADD CONSTRAINT prepared_stock_pkey PRIMARY KEY (product_id);

ALTER TABLE product_option_links ADD CONSTRAINT product_option_links_pkey PRIMARY KEY (product_id, option_id);

ALTER TABLE product_options ADD CONSTRAINT product_options_pkey PRIMARY KEY (id);

ALTER TABLE production_run_lines ADD CONSTRAINT production_run_lines_pkey PRIMARY KEY (production_run_id, ingredient_id);

ALTER TABLE production_runs ADD CONSTRAINT production_runs_pkey PRIMARY KEY (id);

ALTER TABLE products ADD CONSTRAINT products_pkey PRIMARY KEY (id);

ALTER TABLE promotion_products ADD CONSTRAINT promotion_products_pkey PRIMARY KEY (promotion_id, product_id);

ALTER TABLE promotion_redemptions ADD CONSTRAINT promotion_redemptions_pkey PRIMARY KEY (promotion_id, order_id);

ALTER TABLE promotions ADD CONSTRAINT promotions_code_key UNIQUE (code);

ALTER TABLE promotions ADD CONSTRAINT promotions_pkey PRIMARY KEY (id);

ALTER TABLE purchase_invoice_payments ADD CONSTRAINT purchase_invoice_payments_pkey PRIMARY KEY (id);

ALTER TABLE purchase_invoices ADD CONSTRAINT purchase_invoices_pkey PRIMARY KEY (id);

ALTER TABLE purchase_invoices ADD CONSTRAINT purchase_invoices_supplier_id_number_key UNIQUE (supplier_id, "NUMBER");

ALTER TABLE purchase_order_lines ADD CONSTRAINT purchase_order_lines_pkey PRIMARY KEY (id);

ALTER TABLE purchase_order_lines ADD CONSTRAINT purchase_order_lines_purchase_order_id_ingredient_id_key UNIQUE (purchase_order_id, ingredient_id);

ALTER TABLE purchase_orders ADD CONSTRAINT purchase_orders_pkey PRIMARY KEY (id);

ALTER TABLE purchase_receipt_lines ADD CONSTRAINT purchase_receipt_lines_pkey PRIMARY KEY (receipt_id, purchase_line_id);

ALTER TABLE purchase_receipts ADD CONSTRAINT purchase_receipts_pkey PRIMARY KEY (id);

ALTER TABLE purchase_request_lines ADD CONSTRAINT purchase_request_lines_pkey PRIMARY KEY (request_id, ingredient_id);

ALTER TABLE purchase_requests ADD CONSTRAINT purchase_requests_pkey PRIMARY KEY (id);

ALTER TABLE recipe_ingredients ADD CONSTRAINT recipe_ingredients_pkey PRIMARY KEY (recipe_id, ingredient_id);

ALTER TABLE recipes ADD CONSTRAINT recipes_pkey PRIMARY KEY (id);

ALTER TABLE recipes ADD CONSTRAINT recipes_product_id_version_key UNIQUE (product_id, version);

ALTER TABLE reservation_events ADD CONSTRAINT reservation_events_pkey PRIMARY KEY (id);

ALTER TABLE reservation_tables ADD CONSTRAINT reservation_tables_pkey PRIMARY KEY (reservation_id, table_id);

ALTER TABLE reservations ADD CONSTRAINT reservations_pkey PRIMARY KEY (id);

ALTER TABLE supplier_ingredients ADD CONSTRAINT supplier_ingredients_pkey PRIMARY KEY (supplier_id, ingredient_id);

ALTER TABLE suppliers ADD CONSTRAINT suppliers_pkey PRIMARY KEY (id);

ALTER TABLE visit_orders ADD CONSTRAINT visit_orders_order_id_key UNIQUE (order_id);

ALTER TABLE visit_orders ADD CONSTRAINT visit_orders_pkey PRIMARY KEY (visit_id, order_id);

ALTER TABLE visit_tables ADD CONSTRAINT visit_tables_pkey PRIMARY KEY (visit_id, table_id);

ALTER TABLE visits ADD CONSTRAINT visits_pkey PRIMARY KEY (id);

ALTER TABLE visits ADD CONSTRAINT visits_reservation_id_key UNIQUE (reservation_id);

ALTER TABLE warehouse_balances ADD CONSTRAINT warehouse_balances_pkey PRIMARY KEY (warehouse_id, ingredient_id);

ALTER TABLE warehouses ADD CONSTRAINT warehouses_location_id_name_key UNIQUE (location_id, name);

ALTER TABLE warehouses ADD CONSTRAINT warehouses_pkey PRIMARY KEY (id);

ALTER TABLE work_shifts ADD CONSTRAINT work_shifts_pkey PRIMARY KEY (id);

ALTER TABLE cash_movements ADD CONSTRAINT cash_movements_cash_session_id_fkey FOREIGN KEY (cash_session_id) REFERENCES cash_sessions(id);

ALTER TABLE cash_sessions ADD CONSTRAINT cash_sessions_employee_id_fkey FOREIGN KEY (employee_id) REFERENCES employees(id);

ALTER TABLE cash_sessions ADD CONSTRAINT cash_sessions_location_id_fkey FOREIGN KEY (location_id) REFERENCES locations(id);

ALTER TABLE cash_sessions ADD CONSTRAINT cash_sessions_shift_id_fkey FOREIGN KEY (shift_id) REFERENCES work_shifts(id);

ALTER TABLE courier_zones ADD CONSTRAINT courier_zones_employee_id_fkey FOREIGN KEY (employee_id) REFERENCES employees(id);

ALTER TABLE courier_zones ADD CONSTRAINT courier_zones_zone_id_fkey FOREIGN KEY (zone_id) REFERENCES delivery_zones(id);

ALTER TABLE customer_orders ADD CONSTRAINT customer_orders_customer_id_fkey FOREIGN KEY (customer_id) REFERENCES customers(id);

ALTER TABLE customer_orders ADD CONSTRAINT customer_orders_location_id_fkey FOREIGN KEY (location_id) REFERENCES locations(id);

ALTER TABLE delivery_incidents ADD CONSTRAINT delivery_incidents_dispatch_id_fkey FOREIGN KEY (dispatch_id) REFERENCES dispatches(id);

ALTER TABLE dining_tables ADD CONSTRAINT dining_tables_location_id_fkey FOREIGN KEY (location_id) REFERENCES locations(id);

ALTER TABLE dispatch_assignments ADD CONSTRAINT dispatch_assignments_dispatch_id_fkey FOREIGN KEY (dispatch_id) REFERENCES dispatches(id);

ALTER TABLE dispatch_assignments ADD CONSTRAINT dispatch_assignments_employee_id_fkey FOREIGN KEY (employee_id) REFERENCES employees(id);

ALTER TABLE dispatch_events ADD CONSTRAINT dispatch_events_dispatch_id_fkey FOREIGN KEY (dispatch_id) REFERENCES dispatches(id);

ALTER TABLE dispatch_zones ADD CONSTRAINT dispatch_zones_dispatch_id_fkey FOREIGN KEY (dispatch_id) REFERENCES dispatches(id);

ALTER TABLE dispatch_zones ADD CONSTRAINT dispatch_zones_zone_id_fkey FOREIGN KEY (zone_id) REFERENCES delivery_zones(id);

ALTER TABLE dispatches ADD CONSTRAINT dispatches_location_id_fkey FOREIGN KEY (location_id) REFERENCES locations(id);

ALTER TABLE dispatches ADD CONSTRAINT dispatches_order_id_fkey FOREIGN KEY (order_id) REFERENCES customer_orders(id);

ALTER TABLE employee_locations ADD CONSTRAINT employee_locations_employee_id_fkey FOREIGN KEY (employee_id) REFERENCES employees(id);

ALTER TABLE employee_locations ADD CONSTRAINT employee_locations_location_id_fkey FOREIGN KEY (location_id) REFERENCES locations(id);

ALTER TABLE employee_roles ADD CONSTRAINT employee_roles_employee_id_fkey FOREIGN KEY (employee_id) REFERENCES employees(id);

ALTER TABLE employee_shifts ADD CONSTRAINT employee_shifts_employee_id_fkey FOREIGN KEY (employee_id) REFERENCES employees(id);

ALTER TABLE employee_shifts ADD CONSTRAINT employee_shifts_shift_id_fkey FOREIGN KEY (shift_id) REFERENCES work_shifts(id);

ALTER TABLE inventory_balances ADD CONSTRAINT inventory_balances_ingredient_id_fkey FOREIGN KEY (ingredient_id) REFERENCES ingredients(id);

ALTER TABLE inventory_balances ADD CONSTRAINT inventory_balances_location_id_fkey FOREIGN KEY (location_id) REFERENCES locations(id);

ALTER TABLE inventory_lots ADD CONSTRAINT inventory_lots_ingredient_id_fkey FOREIGN KEY (ingredient_id) REFERENCES ingredients(id);

ALTER TABLE inventory_lots ADD CONSTRAINT inventory_lots_warehouse_id_fkey FOREIGN KEY (warehouse_id) REFERENCES warehouses(id);

ALTER TABLE inventory_movement_lines ADD CONSTRAINT inventory_movement_lines_ingredient_id_fkey FOREIGN KEY (ingredient_id) REFERENCES ingredients(id);

ALTER TABLE inventory_movement_lines ADD CONSTRAINT inventory_movement_lines_movement_id_fkey FOREIGN KEY (movement_id) REFERENCES inventory_movements(id);

ALTER TABLE inventory_movements ADD CONSTRAINT inventory_movements_location_id_fkey FOREIGN KEY (location_id) REFERENCES locations(id);

ALTER TABLE inventory_movements ADD CONSTRAINT inventory_movements_target_warehouse_id_fkey FOREIGN KEY (target_warehouse_id) REFERENCES warehouses(id);

ALTER TABLE inventory_movements ADD CONSTRAINT inventory_movements_warehouse_id_fkey FOREIGN KEY (warehouse_id) REFERENCES warehouses(id);

ALTER TABLE kitchen_ticket_lines ADD CONSTRAINT kitchen_ticket_lines_order_line_id_fkey FOREIGN KEY (order_line_id) REFERENCES order_lines(id);

ALTER TABLE kitchen_ticket_lines ADD CONSTRAINT kitchen_ticket_lines_ticket_id_fkey FOREIGN KEY (ticket_id) REFERENCES kitchen_tickets(id);

ALTER TABLE kitchen_tickets ADD CONSTRAINT kitchen_tickets_order_id_fkey FOREIGN KEY (order_id) REFERENCES customer_orders(id);

ALTER TABLE loyalty_movements ADD CONSTRAINT loyalty_movements_customer_id_fkey FOREIGN KEY (customer_id) REFERENCES customers(id);

ALTER TABLE loyalty_movements ADD CONSTRAINT loyalty_movements_order_id_fkey FOREIGN KEY (order_id) REFERENCES customer_orders(id);

ALTER TABLE order_discounts ADD CONSTRAINT order_discounts_order_id_fkey FOREIGN KEY (order_id) REFERENCES customer_orders(id);

ALTER TABLE order_inventory_consumptions ADD CONSTRAINT order_inventory_consumptions_ingredient_id_fkey FOREIGN KEY (ingredient_id) REFERENCES ingredients(id);

ALTER TABLE order_inventory_consumptions ADD CONSTRAINT order_inventory_consumptions_order_line_id_fkey FOREIGN KEY (order_line_id) REFERENCES order_lines(id);

ALTER TABLE order_line_options ADD CONSTRAINT order_line_options_option_id_fkey FOREIGN KEY (option_id) REFERENCES product_options(id);

ALTER TABLE order_line_options ADD CONSTRAINT order_line_options_order_line_id_fkey FOREIGN KEY (order_line_id) REFERENCES order_lines(id);

ALTER TABLE order_lines ADD CONSTRAINT order_lines_order_id_fkey FOREIGN KEY (order_id) REFERENCES customer_orders(id);

ALTER TABLE order_lines ADD CONSTRAINT order_lines_product_id_fkey FOREIGN KEY (product_id) REFERENCES products(id);

ALTER TABLE order_prepared_consumptions ADD CONSTRAINT order_prepared_consumptions_order_line_id_fkey FOREIGN KEY (order_line_id) REFERENCES order_lines(id);

ALTER TABLE order_status_events ADD CONSTRAINT order_status_events_order_id_fkey FOREIGN KEY (order_id) REFERENCES customer_orders(id);

ALTER TABLE payment_allocations ADD CONSTRAINT payment_allocations_order_id_fkey FOREIGN KEY (order_id) REFERENCES customer_orders(id);

ALTER TABLE payment_allocations ADD CONSTRAINT payment_allocations_payment_id_fkey FOREIGN KEY (payment_id) REFERENCES payments(id);

ALTER TABLE payment_document_files ADD CONSTRAINT payment_document_files_document_id_fkey FOREIGN KEY (document_id) REFERENCES payment_documents(id);

ALTER TABLE payment_document_lines ADD CONSTRAINT payment_document_lines_document_id_fkey FOREIGN KEY (document_id) REFERENCES payment_documents(id);

ALTER TABLE payment_documents ADD CONSTRAINT payment_documents_order_id_fkey FOREIGN KEY (order_id) REFERENCES customer_orders(id);

ALTER TABLE payment_documents ADD CONSTRAINT payment_documents_payment_id_fkey FOREIGN KEY (payment_id) REFERENCES payments(id);

ALTER TABLE payment_methods ADD CONSTRAINT payment_methods_payment_id_fkey FOREIGN KEY (payment_id) REFERENCES payments(id);

ALTER TABLE payment_refunds ADD CONSTRAINT payment_refunds_cash_session_id_fkey FOREIGN KEY (cash_session_id) REFERENCES cash_sessions(id);

ALTER TABLE payment_refunds ADD CONSTRAINT payment_refunds_order_id_fkey FOREIGN KEY (order_id) REFERENCES customer_orders(id);

ALTER TABLE payment_refunds ADD CONSTRAINT payment_refunds_payment_id_fkey FOREIGN KEY (payment_id) REFERENCES payments(id);

ALTER TABLE payments ADD CONSTRAINT payments_cash_session_id_fkey FOREIGN KEY (cash_session_id) REFERENCES cash_sessions(id);

ALTER TABLE prepared_stock ADD CONSTRAINT prepared_stock_product_id_fkey FOREIGN KEY (product_id) REFERENCES products(id);

ALTER TABLE product_option_links ADD CONSTRAINT product_option_links_option_id_fkey FOREIGN KEY (option_id) REFERENCES product_options(id);

ALTER TABLE product_option_links ADD CONSTRAINT product_option_links_product_id_fkey FOREIGN KEY (product_id) REFERENCES products(id);

ALTER TABLE production_run_lines ADD CONSTRAINT production_run_lines_ingredient_id_fkey FOREIGN KEY (ingredient_id) REFERENCES ingredients(id);

ALTER TABLE production_run_lines ADD CONSTRAINT production_run_lines_production_run_id_fkey FOREIGN KEY (production_run_id) REFERENCES production_runs(id);

ALTER TABLE production_runs ADD CONSTRAINT production_runs_location_id_fkey FOREIGN KEY (location_id) REFERENCES locations(id);

ALTER TABLE production_runs ADD CONSTRAINT production_runs_recipe_id_fkey FOREIGN KEY (recipe_id) REFERENCES recipes(id);

ALTER TABLE promotion_products ADD CONSTRAINT promotion_products_product_id_fkey FOREIGN KEY (product_id) REFERENCES products(id);

ALTER TABLE promotion_products ADD CONSTRAINT promotion_products_promotion_id_fkey FOREIGN KEY (promotion_id) REFERENCES promotions(id);

ALTER TABLE promotion_redemptions ADD CONSTRAINT promotion_redemptions_customer_id_fkey FOREIGN KEY (customer_id) REFERENCES customers(id);

ALTER TABLE promotion_redemptions ADD CONSTRAINT promotion_redemptions_order_id_fkey FOREIGN KEY (order_id) REFERENCES customer_orders(id);

ALTER TABLE promotion_redemptions ADD CONSTRAINT promotion_redemptions_promotion_id_fkey FOREIGN KEY (promotion_id) REFERENCES promotions(id);

ALTER TABLE purchase_invoice_payments ADD CONSTRAINT purchase_invoice_payments_invoice_id_fkey FOREIGN KEY (invoice_id) REFERENCES purchase_invoices(id);

ALTER TABLE purchase_invoices ADD CONSTRAINT purchase_invoices_purchase_order_id_fkey FOREIGN KEY (purchase_order_id) REFERENCES purchase_orders(id);

ALTER TABLE purchase_invoices ADD CONSTRAINT purchase_invoices_supplier_id_fkey FOREIGN KEY (supplier_id) REFERENCES suppliers(id);

ALTER TABLE purchase_order_lines ADD CONSTRAINT purchase_order_lines_ingredient_id_fkey FOREIGN KEY (ingredient_id) REFERENCES ingredients(id);

ALTER TABLE purchase_order_lines ADD CONSTRAINT purchase_order_lines_purchase_order_id_fkey FOREIGN KEY (purchase_order_id) REFERENCES purchase_orders(id);

ALTER TABLE purchase_orders ADD CONSTRAINT purchase_orders_location_id_fkey FOREIGN KEY (location_id) REFERENCES locations(id);

ALTER TABLE purchase_orders ADD CONSTRAINT purchase_orders_request_id_fkey FOREIGN KEY (request_id) REFERENCES purchase_requests(id);

ALTER TABLE purchase_orders ADD CONSTRAINT purchase_orders_supplier_id_fkey FOREIGN KEY (supplier_id) REFERENCES suppliers(id);

ALTER TABLE purchase_receipt_lines ADD CONSTRAINT purchase_receipt_lines_purchase_line_id_fkey FOREIGN KEY (purchase_line_id) REFERENCES purchase_order_lines(id);

ALTER TABLE purchase_receipt_lines ADD CONSTRAINT purchase_receipt_lines_receipt_id_fkey FOREIGN KEY (receipt_id) REFERENCES purchase_receipts(id);

ALTER TABLE purchase_receipts ADD CONSTRAINT purchase_receipts_purchase_order_id_fkey FOREIGN KEY (purchase_order_id) REFERENCES purchase_orders(id);

ALTER TABLE purchase_receipts ADD CONSTRAINT purchase_receipts_warehouse_id_fkey FOREIGN KEY (warehouse_id) REFERENCES warehouses(id);

ALTER TABLE purchase_request_lines ADD CONSTRAINT purchase_request_lines_ingredient_id_fkey FOREIGN KEY (ingredient_id) REFERENCES ingredients(id);

ALTER TABLE purchase_request_lines ADD CONSTRAINT purchase_request_lines_request_id_fkey FOREIGN KEY (request_id) REFERENCES purchase_requests(id);

ALTER TABLE recipe_ingredients ADD CONSTRAINT recipe_ingredients_ingredient_id_fkey FOREIGN KEY (ingredient_id) REFERENCES ingredients(id);

ALTER TABLE recipe_ingredients ADD CONSTRAINT recipe_ingredients_recipe_id_fkey FOREIGN KEY (recipe_id) REFERENCES recipes(id);

ALTER TABLE recipes ADD CONSTRAINT recipes_product_id_fkey FOREIGN KEY (product_id) REFERENCES products(id);

ALTER TABLE reservation_events ADD CONSTRAINT reservation_events_reservation_id_fkey FOREIGN KEY (reservation_id) REFERENCES reservations(id);

ALTER TABLE reservation_tables ADD CONSTRAINT reservation_tables_reservation_id_fkey FOREIGN KEY (reservation_id) REFERENCES reservations(id);

ALTER TABLE reservation_tables ADD CONSTRAINT reservation_tables_table_id_fkey FOREIGN KEY (table_id) REFERENCES dining_tables(id);

ALTER TABLE reservations ADD CONSTRAINT reservations_customer_id_fkey FOREIGN KEY (customer_id) REFERENCES customers(id);

ALTER TABLE supplier_ingredients ADD CONSTRAINT supplier_ingredients_ingredient_id_fkey FOREIGN KEY (ingredient_id) REFERENCES ingredients(id);

ALTER TABLE supplier_ingredients ADD CONSTRAINT supplier_ingredients_supplier_id_fkey FOREIGN KEY (supplier_id) REFERENCES suppliers(id);

ALTER TABLE visit_orders ADD CONSTRAINT visit_orders_order_id_fkey FOREIGN KEY (order_id) REFERENCES customer_orders(id);

ALTER TABLE visit_orders ADD CONSTRAINT visit_orders_visit_id_fkey FOREIGN KEY (visit_id) REFERENCES visits(id);

ALTER TABLE visit_tables ADD CONSTRAINT visit_tables_table_id_fkey FOREIGN KEY (table_id) REFERENCES dining_tables(id);

ALTER TABLE visit_tables ADD CONSTRAINT visit_tables_visit_id_fkey FOREIGN KEY (visit_id) REFERENCES visits(id);

ALTER TABLE visits ADD CONSTRAINT visits_reservation_id_fkey FOREIGN KEY (reservation_id) REFERENCES reservations(id);

ALTER TABLE warehouse_balances ADD CONSTRAINT warehouse_balances_ingredient_id_fkey FOREIGN KEY (ingredient_id) REFERENCES ingredients(id);

ALTER TABLE warehouse_balances ADD CONSTRAINT warehouse_balances_warehouse_id_fkey FOREIGN KEY (warehouse_id) REFERENCES warehouses(id);

ALTER TABLE warehouses ADD CONSTRAINT warehouses_location_id_fkey FOREIGN KEY (location_id) REFERENCES locations(id);

ALTER TABLE work_shifts ADD CONSTRAINT work_shifts_location_id_fkey FOREIGN KEY (location_id) REFERENCES locations(id);

CREATE INDEX idx_cash_payments_date ON payments (created_at DESC);

CREATE INDEX idx_dispatches_status_created ON dispatches (status, created_at);

CREATE INDEX idx_inventory_movements_location_date ON inventory_movements (location_id, created_at DESC);

CREATE INDEX idx_lot_expiry ON inventory_lots (expires_on);

CREATE INDEX idx_order_inventory_consumptions_line ON order_inventory_consumptions (order_line_id);

CREATE INDEX idx_order_lines_order ON order_lines (order_id);

CREATE INDEX idx_orders_customer_history ON customer_orders (customer_subject, created_at DESC);

CREATE INDEX idx_orders_status_created ON customer_orders (status, created_at DESC);

CREATE INDEX idx_production_runs_created ON production_runs (created_at DESC);

CREATE INDEX idx_products_public_catalog ON products (active, available, category, name);

CREATE INDEX idx_purchase_orders_status_created ON purchase_orders (status, created_at);

CREATE INDEX idx_receipt_order ON purchase_receipts (purchase_order_id);

CREATE INDEX idx_refund_order ON payment_refunds (order_id);

CREATE INDEX idx_reservations_starts_status ON reservations (starts_at, status);

CREATE INDEX idx_visit_active ON visits (arrived_at);

CREATE UNIQUE INDEX uq_cash_session_open ON cash_sessions (CASE WHEN ((status) = 'OPEN') THEN location_id END);

CREATE UNIQUE INDEX uq_customer_subject ON customers (CASE WHEN (subject IS NOT NULL) THEN subject END);

CREATE UNIQUE INDEX uq_dispatch_assignment_active ON dispatch_assignments (CASE WHEN (ended_at IS NULL) THEN dispatch_id END);

CREATE UNIQUE INDEX uq_order_request ON customer_orders (CASE WHEN (request_key IS NOT NULL) THEN request_key END);

CREATE UNIQUE INDEX uq_order_tracking ON customer_orders (tracking_token);

CREATE UNIQUE INDEX uq_payment_request ON payments (CASE WHEN (request_key IS NOT NULL) THEN request_key END);

CREATE UNIQUE INDEX uq_points_order_award ON loyalty_movements (CASE WHEN ((reason) = 'COMPRA') THEN order_id END);

CREATE UNIQUE INDEX uq_promotion_code_case ON promotions (CASE WHEN (code IS NOT NULL) THEN upper((code)) END);

CREATE UNIQUE INDEX uq_recipe_active ON recipes (CASE WHEN active = 1 THEN product_id END);

-- Columnas agregadas en las mejoras del sistema (DNI, mesa del pedido, grupos de turno, área de mesa, cuenta de la reserva).
ALTER TABLE customers ADD (dni VARCHAR2(8 CHAR));
ALTER TABLE employees ADD (dni VARCHAR2(8 CHAR), email VARCHAR2(150 CHAR), username VARCHAR2(80 CHAR));
ALTER TABLE customer_orders ADD (table_id NUMBER(19));
ALTER TABLE work_shifts ADD (group_name VARCHAR2(80 CHAR), notes VARCHAR2(300 CHAR));
ALTER TABLE dining_tables ADD (area VARCHAR2(40 CHAR) DEFAULT 'Salón');
ALTER TABLE reservations ADD (customer_subject VARCHAR2(120 CHAR));

-- Local principal y almacén principal (el sistema trabaja con el local 1 y el almacén 1).
INSERT INTO locations (id, name, address) VALUES (1, 'La Rinconada', 'Juliaca');
INSERT INTO warehouses (id, location_id, name) VALUES (1, 1, 'Almacén principal');
COMMIT;
