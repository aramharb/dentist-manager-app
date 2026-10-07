CREATE TABLE material_inventory (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(180) NOT NULL,
    category VARCHAR(100) NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity >= 0),
    unit VARCHAR(50) NOT NULL,
    minimum_stock INTEGER NOT NULL CHECK (minimum_stock >= 0),
    expiration_date DATE,
    supplier VARCHAR(180),
    status VARCHAR(30) NOT NULL CHECK (status IN ('AVAILABLE', 'LOW_STOCK', 'OUT_OF_STOCK')),
    batch_number VARCHAR(100),
    monthly_consumption INTEGER NOT NULL DEFAULT 0 CHECK (monthly_consumption >= 0),
    purchase_cost NUMERIC(12, 2) NOT NULL CHECK (purchase_cost > 0),
    purchase_expense_id BIGINT REFERENCES expense(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_material_inventory_name ON material_inventory(name);
CREATE INDEX idx_material_inventory_status ON material_inventory(status);
CREATE INDEX idx_material_inventory_category ON material_inventory(category);
