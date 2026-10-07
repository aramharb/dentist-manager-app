-- Connects the treatment selected during patient registration to the clinical
-- treatment workspace and supports procedures that apply to the whole mouth.

ALTER TABLE patient
    ADD COLUMN registration_treatment_id BIGINT;

ALTER TABLE treatment_procedure
    ADD COLUMN all_teeth BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE patient p
SET registration_treatment_id = (
    SELECT t.id
    FROM treatment t
    WHERE t.patient_id = p.id
      AND lower(t.objective) = lower(p.current_treatment)
    ORDER BY t.updated_at DESC
    LIMIT 1
)
WHERE p.current_treatment IS NOT NULL
  AND btrim(p.current_treatment) <> '';

WITH created AS (
    INSERT INTO treatment (
        patient_id,
        objective,
        status,
        priority,
        progress_percent,
        estimated_duration_minutes,
        estimated_bill,
        paid_amount,
        last_visit,
        doctor_notes
    )
    SELECT
        p.id,
        p.current_treatment,
        'PLANNED',
        'NORMAL',
        0,
        0,
        p.expected_amount,
        p.paid_amount,
        p.last_visit::timestamp,
        'Created from the patient registration treatment.'
    FROM patient p
    WHERE p.registration_treatment_id IS NULL
      AND p.current_treatment IS NOT NULL
      AND btrim(p.current_treatment) <> ''
    RETURNING id, patient_id
)
UPDATE patient p
SET registration_treatment_id = created.id
FROM created
WHERE p.id = created.patient_id;

ALTER TABLE patient
    ADD CONSTRAINT fk_patient_registration_treatment
        FOREIGN KEY (registration_treatment_id) REFERENCES treatment(id)
        ON UPDATE CASCADE ON DELETE SET NULL;

ALTER TABLE treatment_procedure
    ADD CONSTRAINT chk_procedure_tooth_scope CHECK (
        (all_teeth = TRUE AND tooth_number IS NULL AND tooth_id IS NULL)
        OR all_teeth = FALSE
    );

CREATE INDEX idx_patient_registration_treatment ON patient(registration_treatment_id);
CREATE INDEX idx_treatment_procedure_all_teeth ON treatment_procedure(all_teeth);
