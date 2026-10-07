CREATE TABLE IF NOT EXISTS patient (
    id BIGSERIAL PRIMARY KEY,
    patient_number VARCHAR(20) NOT NULL UNIQUE,
    first_name VARCHAR(80) NOT NULL,
    last_name VARCHAR(80) NOT NULL,
    gender VARCHAR(20),
    birth_date DATE,
    address TEXT,
    phone_number VARCHAR(30) NOT NULL,
    email VARCHAR(120),
    blood_type VARCHAR(10),
    allergies TEXT,
    current_treatment TEXT,
    cnam_covered BOOLEAN DEFAULT FALSE,
    cnam_number VARCHAR(20),
    first_visit DATE,
    last_visit DATE,
    next_appointment TIMESTAMP,
    unpaid_balance NUMERIC(12, 2) DEFAULT 0,
    notes TEXT,
    last_modified TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_patient_last_name ON patient (last_name);
CREATE INDEX IF NOT EXISTS idx_patient_phone_number ON patient (phone_number);
