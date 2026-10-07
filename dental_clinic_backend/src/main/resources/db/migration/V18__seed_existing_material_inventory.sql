-- Preserve the inventory cards that existed before materials became database-backed.
-- Their historical purchase price is unknown, so it remains zero until assigned once.

ALTER TABLE material_inventory DROP CONSTRAINT IF EXISTS material_inventory_purchase_cost_check;
ALTER TABLE material_inventory ADD CONSTRAINT material_inventory_purchase_cost_check
    CHECK (purchase_cost >= 0);

INSERT INTO material_inventory (
    name, category, quantity, unit, minimum_stock, expiration_date, supplier, status,
    batch_number, monthly_consumption, purchase_cost
) VALUES
    ('Composite Resin A2', 'Restorative', 18, 'syringes', 8, '2027-02-10', 'DentPlus', 'AVAILABLE', 'DP-A2-771', 21, 0),
    ('Nitrile Gloves M', 'Protection', 6, 'boxes', 10, '2028-05-02', 'MedSupply', 'LOW_STOCK', 'MS-GM-204', 34, 0),
    ('Anesthetic Carpules', 'Medication', 0, 'packs', 5, '2026-12-21', 'PharmaDent', 'OUT_OF_STOCK', 'PD-AN-087', 15, 0),
    ('Impression Material', 'Prosthetics', 12, 'kits', 4, '2027-01-15', 'OrthoLine', 'AVAILABLE', 'OL-IM-552', 8, 0),
    ('Sterilization Pouches', 'Sterilization', 9, 'packs', 12, '2029-08-19', 'CleanCare', 'LOW_STOCK', 'CC-SP-919', 29, 0);
