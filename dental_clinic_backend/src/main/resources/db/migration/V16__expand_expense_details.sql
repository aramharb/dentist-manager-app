-- Persist every field shown by the secretary/doctor expense forms.

ALTER TABLE expense DROP CONSTRAINT IF EXISTS expense_category_check;
ALTER TABLE expense DROP CONSTRAINT IF EXISTS expense_status_check;
ALTER TABLE expense ALTER COLUMN description TYPE VARCHAR(1000);
ALTER TABLE expense ALTER COLUMN billing_period_months SET DEFAULT 1;

ALTER TABLE expense ADD COLUMN expense_date DATE;
ALTER TABLE expense ADD COLUMN due_date DATE;
ALTER TABLE expense ADD COLUMN paid_at TIMESTAMP;
ALTER TABLE expense ADD COLUMN label VARCHAR(255);
ALTER TABLE expense ADD COLUMN supplier VARCHAR(255);
ALTER TABLE expense ADD COLUMN invoice_number VARCHAR(100);
ALTER TABLE expense ADD COLUMN owner VARCHAR(150);
ALTER TABLE expense ADD COLUMN source_role VARCHAR(20) NOT NULL DEFAULT 'SECRETARY';
ALTER TABLE expense ADD COLUMN entered_by VARCHAR(150);
ALTER TABLE expense ADD COLUMN unexpected BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE expense ADD COLUMN unexpected_note VARCHAR(2000);
ALTER TABLE expense ADD COLUMN payment_method VARCHAR(100);
ALTER TABLE expense ADD COLUMN tax_deductible BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE expense ADD COLUMN recurring BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE expense ADD COLUMN attachment_url VARCHAR(1000);
ALTER TABLE expense ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

UPDATE expense
SET expense_date = CAST(recorded_at AS DATE),
    label = COALESCE(NULLIF(description, ''), REPLACE(category, '_', ' '));

ALTER TABLE expense ALTER COLUMN expense_date SET NOT NULL;
ALTER TABLE expense ALTER COLUMN label SET NOT NULL;

CREATE INDEX idx_expense_expense_date ON expense(expense_date DESC);
CREATE INDEX idx_expense_status ON expense(status);
