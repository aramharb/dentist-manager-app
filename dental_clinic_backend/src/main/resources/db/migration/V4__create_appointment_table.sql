CREATE TABLE IF NOT EXISTS appointment (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL REFERENCES patient(id),
    treatment_id BIGINT REFERENCES treatment(id),
    date DATE NOT NULL,
    heure TIME NOT NULL,
    duration_minutes INTEGER NOT NULL DEFAULT 30,
    provider_name VARCHAR(120) NOT NULL,
    priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    status VARCHAR(30) NOT NULL DEFAULT 'SCHEDULED',
    notes TEXT
);

CREATE INDEX IF NOT EXISTS idx_appointment_patient_id ON appointment (patient_id);
CREATE INDEX IF NOT EXISTS idx_appointment_treatment_id ON appointment (treatment_id);
CREATE INDEX IF NOT EXISTS idx_appointment_date ON appointment (date);
CREATE INDEX IF NOT EXISTS idx_appointment_status ON appointment (status);
