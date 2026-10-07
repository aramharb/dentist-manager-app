CREATE TABLE IF NOT EXISTS treatment_type (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    description TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS procedure_catalog (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    name VARCHAR(140) NOT NULL,
    category VARCHAR(80) NOT NULL,
    default_cost NUMERIC(12, 2) NOT NULL DEFAULT 0 CHECK (default_cost >= 0),
    default_duration_minutes INTEGER NOT NULL DEFAULT 30 CHECK (default_duration_minutes > 0),
    description TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tooth (
    id BIGSERIAL PRIMARY KEY,
    fdi_number SMALLINT NOT NULL UNIQUE CHECK (fdi_number IN (
        11,12,13,14,15,16,17,18,21,22,23,24,25,26,27,28,
        31,32,33,34,35,36,37,38,41,42,43,44,45,46,47,48
    )),
    quadrant SMALLINT NOT NULL CHECK (quadrant BETWEEN 1 AND 4),
    position SMALLINT NOT NULL CHECK (position BETWEEN 1 AND 8),
    jaw VARCHAR(10) NOT NULL CHECK (jaw IN ('UPPER', 'LOWER')),
    label VARCHAR(40) NOT NULL
);

CREATE TABLE IF NOT EXISTS treatment (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    treatment_type_id BIGINT,
    objective VARCHAR(220) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PLANNED' CHECK (status IN ('PLANNED','IN_PROGRESS','COMPLETED','ON_HOLD','CANCELLED')),
    priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL' CHECK (priority IN ('LOW','NORMAL','HIGH','URGENT')),
    progress_percent INTEGER NOT NULL DEFAULT 0 CHECK (progress_percent BETWEEN 0 AND 100),
    estimated_duration_minutes INTEGER NOT NULL DEFAULT 0 CHECK (estimated_duration_minutes >= 0),
    estimated_bill NUMERIC(12, 2) NOT NULL DEFAULT 0 CHECK (estimated_bill >= 0),
    paid_amount NUMERIC(12, 2) NOT NULL DEFAULT 0 CHECK (paid_amount >= 0),
    upcoming_appointment TIMESTAMP,
    last_visit TIMESTAMP,
    doctor_notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_treatment_patient FOREIGN KEY (patient_id) REFERENCES patient(id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_treatment_type FOREIGN KEY (treatment_type_id) REFERENCES treatment_type(id) ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT chk_treatment_payment CHECK (paid_amount <= estimated_bill OR estimated_bill = 0)
);

CREATE TABLE IF NOT EXISTS treatment_procedure (
    id BIGSERIAL PRIMARY KEY,
    treatment_id BIGINT NOT NULL,
    procedure_catalog_id BIGINT,
    tooth_id BIGINT,
    tooth_number SMALLINT CHECK (tooth_number IN (
        11,12,13,14,15,16,17,18,21,22,23,24,25,26,27,28,
        31,32,33,34,35,36,37,38,41,42,43,44,45,46,47,48
    )),
    name VARCHAR(140) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PLANNED' CHECK (status IN ('PLANNED','IN_PROGRESS','COMPLETED','CANCELLED')),
    practitioner VARCHAR(120),
    cost NUMERIC(12, 2) NOT NULL DEFAULT 0 CHECK (cost >= 0),
    duration_minutes INTEGER NOT NULL DEFAULT 30 CHECK (duration_minutes > 0),
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tp_treatment FOREIGN KEY (treatment_id) REFERENCES treatment(id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_tp_catalog FOREIGN KEY (procedure_catalog_id) REFERENCES procedure_catalog(id) ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_tp_tooth FOREIGN KEY (tooth_id) REFERENCES tooth(id) ON UPDATE CASCADE ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS treatment_history (
    id BIGSERIAL PRIMARY KEY,
    treatment_id BIGINT NOT NULL,
    event_type VARCHAR(40) NOT NULL CHECK (event_type IN ('CREATED','VISIT','PHOTO_ADDED','PROCEDURE_COMPLETED','PRESCRIPTION_GENERATED','PAYMENT','COMPLETED','NOTE')),
    title VARCHAR(160) NOT NULL,
    description TEXT,
    event_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(120),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_history_treatment FOREIGN KEY (treatment_id) REFERENCES treatment(id) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS treatment_photo (
    id BIGSERIAL PRIMARY KEY,
    treatment_id BIGINT NOT NULL,
    photo_type VARCHAR(20) NOT NULL CHECK (photo_type IN ('BEFORE','AFTER','XRAY','SCAN','OTHER')),
    file_name VARCHAR(220) NOT NULL,
    content_type VARCHAR(120) NOT NULL,
    url TEXT NOT NULL,
    description TEXT,
    uploaded_by VARCHAR(120),
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_photo_treatment FOREIGN KEY (treatment_id) REFERENCES treatment(id) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS prescription (
    id BIGSERIAL PRIMARY KEY,
    treatment_id BIGINT NOT NULL,
    medicine_name VARCHAR(160) NOT NULL,
    dosage VARCHAR(120) NOT NULL,
    duration VARCHAR(120) NOT NULL,
    instructions TEXT,
    issued_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    pdf_url TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_prescription_treatment FOREIGN KEY (treatment_id) REFERENCES treatment(id) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS payment (
    id BIGSERIAL PRIMARY KEY,
    treatment_id BIGINT NOT NULL,
    amount NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    method VARCHAR(40) NOT NULL DEFAULT 'CASH',
    paid_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reference VARCHAR(120),
    notes TEXT,
    CONSTRAINT fk_payment_treatment FOREIGN KEY (treatment_id) REFERENCES treatment(id) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_treatment_patient ON treatment(patient_id);
CREATE INDEX IF NOT EXISTS idx_treatment_status ON treatment(status);
CREATE INDEX IF NOT EXISTS idx_treatment_priority ON treatment(priority);
CREATE INDEX IF NOT EXISTS idx_tp_treatment ON treatment_procedure(treatment_id);
CREATE INDEX IF NOT EXISTS idx_tp_tooth_number ON treatment_procedure(tooth_number);
CREATE INDEX IF NOT EXISTS idx_history_treatment_event_at ON treatment_history(treatment_id, event_at DESC);
CREATE INDEX IF NOT EXISTS idx_photo_treatment ON treatment_photo(treatment_id);
CREATE INDEX IF NOT EXISTS idx_prescription_treatment ON prescription(treatment_id);
CREATE INDEX IF NOT EXISTS idx_payment_treatment ON payment(treatment_id);

INSERT INTO procedure_catalog (code, name, category, default_cost, default_duration_minutes)
VALUES
    ('CLEANING', 'Cleaning', 'Preventive', 80, 30),
    ('SCALING', 'Scaling', 'Periodontics', 120, 45),
    ('EXTRACTION', 'Extraction', 'Surgery', 180, 45),
    ('ROOT_CANAL', 'Root Canal', 'Endodontics', 420, 90),
    ('COMPOSITE_FILLING', 'Composite Filling', 'Restorative', 160, 40),
    ('CROWN', 'Crown', 'Prosthodontics', 650, 75),
    ('BRIDGE', 'Bridge', 'Prosthodontics', 1200, 120),
    ('IMPLANT', 'Implant', 'Implantology', 1800, 120),
    ('WHITENING', 'Whitening', 'Cosmetic', 300, 60),
    ('ORTHODONTICS', 'Orthodontics', 'Orthodontics', 1500, 60),
    ('PEDIATRIC_CARE', 'Pediatric Care', 'Pediatric', 90, 30)
ON CONFLICT (code) DO NOTHING;

INSERT INTO treatment_type (code, name)
VALUES
    ('GENERAL', 'General Dentistry'),
    ('RESTORATIVE', 'Restorative Plan'),
    ('ORTHODONTIC', 'Orthodontic Plan'),
    ('SURGICAL', 'Surgical Plan')
ON CONFLICT (code) DO NOTHING;

INSERT INTO tooth (fdi_number, quadrant, position, jaw, label)
SELECT value, value / 10, value % 10, CASE WHEN value / 10 IN (1, 2) THEN 'UPPER' ELSE 'LOWER' END, 'Tooth ' || value
FROM unnest(ARRAY[18,17,16,15,14,13,12,11,21,22,23,24,25,26,27,28,48,47,46,45,44,43,42,41,31,32,33,34,35,36,37,38]) AS value
ON CONFLICT (fdi_number) DO NOTHING;
