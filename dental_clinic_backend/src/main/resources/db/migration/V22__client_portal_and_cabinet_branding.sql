-- Cabinet identity (shown on the public cabinet page and the client space) ...
ALTER TABLE cabinet
    ADD COLUMN owner_name VARCHAR(160),
    ADD COLUMN tagline VARCHAR(200),
    ADD COLUMN primary_color CHAR(7) NOT NULL DEFAULT '#1689E8'
        CONSTRAINT cabinet_primary_color_check CHECK (primary_color ~ '^#[0-9A-Fa-f]{6}$');

-- ... with its logo and cover photo, stored in the database (small, size-limited images).
CREATE TABLE cabinet_image (
    cabinet_id BIGINT NOT NULL REFERENCES cabinet(id) ON DELETE CASCADE,
    kind VARCHAR(10) NOT NULL CHECK (kind IN ('logo', 'cover')),
    content_type VARCHAR(40) NOT NULL,
    data BYTEA NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (cabinet_id, kind)
);

-- Client (patient) accounts. One account per phone number; the client can join several
-- cabinets. Passwords are stored as salted PBKDF2 hashes.
CREATE TABLE client_account (
    id BIGSERIAL PRIMARY KEY,
    phone VARCHAR(20) NOT NULL UNIQUE,
    full_name VARCHAR(160) NOT NULL,
    password_hash VARCHAR(200) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- A client's file request in one cabinet: the details they typed (same fields as "add client").
-- patient_id stays empty until a secretary of that cabinet links it to a client file.
CREATE TABLE client_membership (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL REFERENCES client_account(id) ON DELETE CASCADE,
    cabinet_id BIGINT NOT NULL REFERENCES cabinet(id),
    status VARCHAR(10) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'LINKED', 'REJECTED')),
    patient_id BIGINT,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    gender VARCHAR(10) NOT NULL CHECK (gender IN ('Male', 'Female')),
    birth_date DATE,
    address VARCHAR(255),
    email VARCHAR(150),
    blood_type VARCHAR(5) CHECK (blood_type IN ('A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-')),
    allergies TEXT,
    cnam_covered BOOLEAN NOT NULL DEFAULT FALSE,
    cnam_number VARCHAR(30),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_membership_account_cabinet UNIQUE (account_id, cabinet_id),
    CONSTRAINT fk_membership_patient_same_cabinet
        FOREIGN KEY (patient_id, cabinet_id) REFERENCES patient(id, cabinet_id)
);
CREATE INDEX idx_membership_cabinet_status ON client_membership(cabinet_id, status);

-- Appointment requests made by clients; a secretary of the cabinet confirms or rejects them.
-- Confirming creates the real appointment (appointment_id).
CREATE TABLE appointment_request (
    id BIGSERIAL PRIMARY KEY,
    cabinet_id BIGINT NOT NULL REFERENCES cabinet(id),
    membership_id BIGINT NOT NULL REFERENCES client_membership(id) ON DELETE CASCADE,
    doctor_user_id BIGINT NOT NULL,
    date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL CHECK (end_time > start_time),
    note VARCHAR(500),
    status VARCHAR(10) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'CONFIRMED', 'REJECTED', 'CANCELLED')),
    reject_reason VARCHAR(300),
    appointment_id BIGINT REFERENCES appointment(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    decided_at TIMESTAMP,
    decided_by BIGINT REFERENCES login(id) ON DELETE SET NULL,
    CONSTRAINT fk_request_doctor_same_cabinet
        FOREIGN KEY (doctor_user_id, cabinet_id) REFERENCES login(id, cabinet_id)
);
CREATE INDEX idx_request_cabinet_status ON appointment_request(cabinet_id, status, date);
CREATE INDEX idx_request_doctor_date ON appointment_request(doctor_user_id, date) WHERE status = 'PENDING';
CREATE INDEX idx_request_membership ON appointment_request(membership_id);
