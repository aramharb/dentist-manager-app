CREATE TABLE IF NOT EXISTS expense (
    id BIGSERIAL PRIMARY KEY,
    expense_date DATE NOT NULL DEFAULT CURRENT_DATE,
    due_date DATE,
    paid_at TIMESTAMP,
    category VARCHAR(80) NOT NULL,
    label VARCHAR(160) NOT NULL,
    description TEXT,
    supplier VARCHAR(140),
    invoice_number VARCHAR(80),
    amount NUMERIC(12, 2) NOT NULL CHECK (amount >= 0),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PAID','PENDING','SCHEDULED','CANCELLED')),
    owner VARCHAR(120),
    source_role VARCHAR(30) NOT NULL DEFAULT 'SECRETARY' CHECK (source_role IN ('SECRETARY','DOCTOR','SYSTEM')),
    entered_by VARCHAR(120),
    unexpected BOOLEAN NOT NULL DEFAULT FALSE,
    unexpected_note TEXT,
    payment_method VARCHAR(40),
    tax_deductible BOOLEAN NOT NULL DEFAULT TRUE,
    recurring BOOLEAN NOT NULL DEFAULT FALSE,
    attachment_url TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_expense_unexpected_note CHECK (unexpected = FALSE OR unexpected_note IS NOT NULL OR description IS NOT NULL)
);

CREATE INDEX IF NOT EXISTS idx_expense_date ON expense(expense_date DESC);
CREATE INDEX IF NOT EXISTS idx_expense_due_date ON expense(due_date);
CREATE INDEX IF NOT EXISTS idx_expense_category ON expense(category);
CREATE INDEX IF NOT EXISTS idx_expense_status ON expense(status);
CREATE INDEX IF NOT EXISTS idx_expense_source_role ON expense(source_role);
CREATE INDEX IF NOT EXISTS idx_expense_invoice_number ON expense(invoice_number);

INSERT INTO expense (expense_date, due_date, category, label, description, supplier, invoice_number, amount, status, owner, source_role, entered_by, unexpected, unexpected_note, payment_method, tax_deductible, recurring)
VALUES
    ('2026-08-04', '2026-08-04', 'Dental Materials', 'Composite resin and polishing discs', 'Restorative materials for active crown and filling cases.', 'DentPlus', 'DP-7781', 960, 'PAID', 'Inventory', 'SECRETARY', 'Nour Belkacem', FALSE, NULL, 'Bank Transfer', TRUE, FALSE),
    ('2026-08-03', '2026-08-10', 'Laboratory', 'Zirconia crown fabrication', 'Lab work for posterior crown case.', 'SmileLab', 'SL-2104', 420, 'PENDING', 'Operations', 'SECRETARY', 'Nour Belkacem', FALSE, NULL, 'Bank Transfer', TRUE, FALSE),
    ('2026-08-02', '2026-08-09', 'Maintenance', 'Compressor preventive service', 'Chair compressor maintenance and calibration.', 'MedTech Service', 'MT-662', 310, 'SCHEDULED', 'Operations', 'SECRETARY', 'Nour Belkacem', FALSE, NULL, 'Cash', TRUE, FALSE),
    ('2026-08-01', '2026-08-15', 'Electricity', 'Monthly electricity bill', 'Clinic electricity utility bill.', 'Utility Office', NULL, 275, 'SCHEDULED', 'Administration', 'SECRETARY', 'Nour Belkacem', FALSE, NULL, 'Bank Transfer', TRUE, TRUE),
    ('2026-07-30', '2026-07-30', 'Miscellaneous', 'Urgent courier for surgical guide', 'Same-day implant guide delivery.', 'City Express', NULL, 95, 'PAID', 'Clinical', 'SECRETARY', 'Nour Belkacem', TRUE, 'Other / Unexpected Expense: same-day implant guide delivery', 'Cash', TRUE, FALSE),
    ('2026-08-06', '2026-08-06', 'Internet', 'Fiber internet connection', 'Monthly fiber internet subscription.', 'Telecom Provider', NULL, 85, 'SCHEDULED', 'Reception', 'SYSTEM', 'system', FALSE, NULL, 'Bank Transfer', TRUE, TRUE),
    ('2026-08-25', '2026-08-25', 'Taxes', 'Patente tax provision', 'Monthly provision for local business tax.', 'Tax Office', NULL, 640, 'PENDING', 'Administration', 'DOCTOR', 'Dr. Wajih', FALSE, NULL, 'Bank Transfer', TRUE, FALSE)
ON CONFLICT DO NOTHING;
