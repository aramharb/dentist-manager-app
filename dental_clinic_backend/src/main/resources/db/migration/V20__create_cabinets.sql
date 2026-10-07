-- Multi-cabinet support.
--
-- A cabinet (clinic) is a closed group of doctors and secretaries. Every piece of
-- business data belongs to exactly one cabinet; users of one cabinet can never see
-- or reach the data, staff or conversations of another. The platform admin account
-- is the only user that belongs to no cabinet and manages all of them.

CREATE TABLE cabinet (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    code VARCHAR(40) NOT NULL,
    address VARCHAR(255),
    phone_number VARCHAR(30),
    email VARCHAR(150),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_cabinet_code UNIQUE (code),
    CONSTRAINT uq_cabinet_name UNIQUE (name)
);

-- Everything that existed before multi-cabinet support moves into this cabinet.
INSERT INTO cabinet (name, code) VALUES ('Main Cabinet', 'MAIN');

-- ---------------------------------------------------------------- users
ALTER TABLE login ADD COLUMN cabinet_id BIGINT;
UPDATE login SET cabinet_id = (SELECT id FROM cabinet WHERE code = 'MAIN') WHERE role <> 'admin';
ALTER TABLE login
    ADD CONSTRAINT fk_login_cabinet FOREIGN KEY (cabinet_id) REFERENCES cabinet(id),
    ADD CONSTRAINT login_cabinet_role_check CHECK (
        (role = 'admin' AND cabinet_id IS NULL) OR (role <> 'admin' AND cabinet_id IS NOT NULL)),
    ADD CONSTRAINT uq_login_id_cabinet UNIQUE (id, cabinet_id);
CREATE INDEX idx_login_cabinet ON login(cabinet_id);

-- ---------------------------------------------------------------- tenant data
-- Adds a NOT NULL cabinet_id (back-filled with the main cabinet) to a data table.
DO $$
DECLARE
    main_id BIGINT := (SELECT id FROM cabinet WHERE code = 'MAIN');
    tbl TEXT;
BEGIN
    FOREACH tbl IN ARRAY ARRAY[
        'patient', 'appointment', 'expense', 'material_inventory', 'procedure_catalog',
        'treatment', 'treatment_procedure', 'treatment_history', 'treatment_photo',
        'prescription', 'staff_action', 'conversation']
    LOOP
        EXECUTE format('ALTER TABLE %I ADD COLUMN cabinet_id BIGINT', tbl);
        EXECUTE format('UPDATE %I SET cabinet_id = %s', tbl, main_id);
        EXECUTE format('ALTER TABLE %I ALTER COLUMN cabinet_id SET NOT NULL', tbl);
        EXECUTE format('ALTER TABLE %I ADD CONSTRAINT %I FOREIGN KEY (cabinet_id) REFERENCES cabinet(id)',
                tbl, 'fk_' || tbl || '_cabinet');
        EXECUTE format('CREATE INDEX %I ON %I(cabinet_id)', 'idx_' || tbl || '_cabinet', tbl);
    END LOOP;
END $$;

-- Numbers and codes were globally unique; they are now unique inside a cabinet.
ALTER TABLE patient DROP CONSTRAINT IF EXISTS patient_patient_number_key;
ALTER TABLE patient ADD CONSTRAINT uq_patient_cabinet_number UNIQUE (cabinet_id, patient_number);
ALTER TABLE procedure_catalog DROP CONSTRAINT IF EXISTS procedure_catalog_code_key;
ALTER TABLE procedure_catalog ADD CONSTRAINT uq_procedure_catalog_cabinet_code UNIQUE (cabinet_id, code);

-- ---------------------------------------------------------------- integrity across cabinets
-- Composite foreign keys make it impossible, even for buggy code, to link rows that
-- live in different cabinets.
ALTER TABLE patient   ADD CONSTRAINT uq_patient_id_cabinet   UNIQUE (id, cabinet_id);
ALTER TABLE treatment ADD CONSTRAINT uq_treatment_id_cabinet UNIQUE (id, cabinet_id);

ALTER TABLE patient
    ADD CONSTRAINT fk_patient_doctor_same_cabinet
        FOREIGN KEY (assigned_doctor_user_id, cabinet_id) REFERENCES login(id, cabinet_id);
ALTER TABLE treatment
    ADD CONSTRAINT fk_treatment_doctor_same_cabinet
        FOREIGN KEY (doctor_user_id, cabinet_id) REFERENCES login(id, cabinet_id);
ALTER TABLE treatment
    ADD CONSTRAINT fk_treatment_patient_same_cabinet
        FOREIGN KEY (patient_id, cabinet_id) REFERENCES patient(id, cabinet_id) ON DELETE CASCADE;
ALTER TABLE appointment
    ADD CONSTRAINT fk_appointment_patient_same_cabinet
        FOREIGN KEY (patient_id, cabinet_id) REFERENCES patient(id, cabinet_id);
ALTER TABLE treatment_procedure
    ADD CONSTRAINT fk_tp_treatment_same_cabinet
        FOREIGN KEY (treatment_id, cabinet_id) REFERENCES treatment(id, cabinet_id) ON DELETE CASCADE;
ALTER TABLE treatment_history
    ADD CONSTRAINT fk_history_treatment_same_cabinet
        FOREIGN KEY (treatment_id, cabinet_id) REFERENCES treatment(id, cabinet_id) ON DELETE CASCADE;
ALTER TABLE treatment_photo
    ADD CONSTRAINT fk_photo_treatment_same_cabinet
        FOREIGN KEY (treatment_id, cabinet_id) REFERENCES treatment(id, cabinet_id) ON DELETE CASCADE;
ALTER TABLE prescription
    ADD CONSTRAINT fk_prescription_treatment_same_cabinet
        FOREIGN KEY (treatment_id, cabinet_id) REFERENCES treatment(id, cabinet_id) ON DELETE CASCADE;
