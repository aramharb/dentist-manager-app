-- Treatment ownership is independent from online presence. Existing records are
-- assigned from their latest appointment when possible, then to an active doctor.

ALTER TABLE patient ADD COLUMN assigned_doctor_user_id BIGINT;
ALTER TABLE treatment ADD COLUMN doctor_user_id BIGINT;
ALTER TABLE treatment_procedure ADD COLUMN completed_by_user_id BIGINT;

UPDATE patient p
SET assigned_doctor_user_id = (
    SELECT a.provider_user_id
    FROM appointment a
    JOIN login u ON u.id = a.provider_user_id
    WHERE a.patient_id = p.id
      AND a.provider_user_id IS NOT NULL
      AND lower(u.role) = 'doctor'
      AND u.active = TRUE
    ORDER BY a.date DESC, a.heure DESC, a.id DESC
    LIMIT 1
);

UPDATE patient
SET assigned_doctor_user_id = (
    SELECT id FROM login
    WHERE lower(role) = 'doctor' AND active = TRUE
    ORDER BY id
    LIMIT 1
)
WHERE assigned_doctor_user_id IS NULL;

UPDATE treatment t
SET doctor_user_id = p.assigned_doctor_user_id
FROM patient p
WHERE p.id = t.patient_id
  AND t.doctor_user_id IS NULL;

ALTER TABLE patient
    ADD CONSTRAINT fk_patient_assigned_doctor
        FOREIGN KEY (assigned_doctor_user_id) REFERENCES login(id)
        ON UPDATE CASCADE ON DELETE SET NULL;

ALTER TABLE treatment
    ADD CONSTRAINT fk_treatment_doctor
        FOREIGN KEY (doctor_user_id) REFERENCES login(id)
        ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE treatment_procedure
    ADD CONSTRAINT fk_treatment_procedure_completed_by
        FOREIGN KEY (completed_by_user_id) REFERENCES login(id)
        ON UPDATE CASCADE ON DELETE SET NULL;

CREATE INDEX idx_patient_assigned_doctor ON patient(assigned_doctor_user_id);
CREATE INDEX idx_treatment_doctor_patient ON treatment(doctor_user_id, patient_id, updated_at DESC);
CREATE INDEX idx_treatment_procedure_status ON treatment_procedure(treatment_id, status);
