-- Adds the treatment and payment snapshot used by the patient registry.
-- Predefined prices remain owned by procedure_catalog; custom treatments keep
-- their expected amount directly on the patient record.

INSERT INTO procedure_catalog (code, name, category, default_cost, default_duration_minutes)
VALUES
    ('CHECKUP', 'Dental Check-up', 'Preventive', 50, 30),
    ('SCALING', 'Teeth Cleaning (Scaling)', 'Preventive', 120, 45),
    ('COMPOSITE_FILLING', 'Dental Filling', 'Restorative', 160, 40),
    ('ROOT_CANAL', 'Root Canal Treatment', 'Endodontics', 420, 90),
    ('EXTRACTION', 'Tooth Extraction', 'Surgery', 180, 45),
    ('WISDOM_EXTRACTION', 'Wisdom Tooth Extraction', 'Surgery', 300, 60),
    ('CROWN', 'Dental Crown', 'Prosthodontics', 650, 75),
    ('BRIDGE', 'Dental Bridge', 'Prosthodontics', 1200, 120),
    ('IMPLANT', 'Dental Implant', 'Implantology', 1800, 120),
    ('DENTURES', 'Dentures', 'Prosthodontics', 900, 90),
    ('ORTHODONTICS', 'Orthodontic Braces', 'Orthodontics', 1500, 60),
    ('INVISALIGN', 'Invisalign', 'Orthodontics', 2500, 60),
    ('VENEERS', 'Dental Veneers', 'Cosmetic', 500, 60),
    ('WHITENING', 'Teeth Whitening', 'Cosmetic', 300, 60),
    ('PERIODONTAL', 'Periodontal Treatment', 'Periodontics', 250, 60),
    ('PEDIATRIC_CARE', 'Pediatric Dentistry', 'Pediatric', 90, 30),
    ('EMERGENCY_CARE', 'Emergency Dental Care', 'Emergency', 100, 30)
ON CONFLICT (code) DO UPDATE SET
    name = EXCLUDED.name,
    category = EXCLUDED.category,
    default_cost = EXCLUDED.default_cost,
    default_duration_minutes = EXCLUDED.default_duration_minutes,
    active = TRUE,
    updated_at = CURRENT_TIMESTAMP;

ALTER TABLE patient
    ADD COLUMN selected_treatment_id BIGINT,
    ADD COLUMN expected_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    ADD COLUMN paid_amount NUMERIC(12, 2) NOT NULL DEFAULT 0;

ALTER TABLE patient
    ADD CONSTRAINT fk_patient_selected_treatment
        FOREIGN KEY (selected_treatment_id) REFERENCES procedure_catalog(id)
        ON UPDATE CASCADE ON DELETE SET NULL,
    ADD CONSTRAINT chk_patient_expected_amount CHECK (expected_amount >= 0),
    ADD CONSTRAINT chk_patient_paid_amount CHECK (paid_amount >= 0),
    ADD CONSTRAINT chk_patient_payment_not_over_expected CHECK (paid_amount <= expected_amount);

UPDATE patient
SET expected_amount = GREATEST(COALESCE(unpaid_balance, 0), 0),
    paid_amount = 0;

ALTER TABLE appointment DROP CONSTRAINT IF EXISTS appointment_patient_id_fkey;
ALTER TABLE appointment DROP CONSTRAINT IF EXISTS appointment_treatment_id_fkey;

ALTER TABLE appointment
    ADD CONSTRAINT appointment_patient_id_fkey
        FOREIGN KEY (patient_id) REFERENCES patient(id) ON DELETE CASCADE,
    ADD CONSTRAINT appointment_treatment_id_fkey
        FOREIGN KEY (treatment_id) REFERENCES treatment(id) ON DELETE SET NULL;

CREATE INDEX idx_patient_selected_treatment ON patient(selected_treatment_id);
