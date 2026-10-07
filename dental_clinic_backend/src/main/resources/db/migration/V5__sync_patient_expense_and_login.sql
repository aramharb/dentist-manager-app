-- Reconciles migration history with schema changes made directly on the database:
--   * patient: tightened column sizes/nullability to match the current entity/UI (gender, blood_type
--     are now constrained value sets; phone_number is optional; next_appointment is date-only)
--   * expense: redesigned from a per-transaction ledger into a fixed recurring-bill tracker
--     (category/status are now constrained value sets; old columns are gone)
--   * login: new table added directly on the database (currently unused by the application code)

ALTER TABLE patient
    ALTER COLUMN first_name TYPE VARCHAR(100),
    ALTER COLUMN last_name TYPE VARCHAR(100),
    ALTER COLUMN gender TYPE VARCHAR(10),
    ALTER COLUMN gender SET NOT NULL,
    ALTER COLUMN phone_number TYPE VARCHAR(20),
    ALTER COLUMN phone_number DROP NOT NULL,
    ALTER COLUMN email TYPE VARCHAR(150),
    ALTER COLUMN blood_type TYPE VARCHAR(5),
    ALTER COLUMN cnam_number TYPE VARCHAR(30),
    ALTER COLUMN next_appointment TYPE DATE USING next_appointment::date,
    ALTER COLUMN unpaid_balance TYPE NUMERIC(10, 2),
    ALTER COLUMN last_modified DROP NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'patient' AND constraint_name = 'patient_gender_check'
    ) THEN
        ALTER TABLE patient ADD CONSTRAINT patient_gender_check
            CHECK (gender IN ('Male', 'Female'));
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'patient' AND constraint_name = 'patient_blood_type_check'
    ) THEN
        ALTER TABLE patient ADD CONSTRAINT patient_blood_type_check
            CHECK (blood_type IN ('A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'));
    END IF;
END $$;

DROP TABLE IF EXISTS expense;

CREATE TABLE expense (
    id BIGSERIAL PRIMARY KEY,
    recorded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    category VARCHAR(40) NOT NULL CHECK (category IN (
        'WATER_BILL', 'ELECTRICITY_BILL', 'INTERNET_BILL', 'PATENTE_BILL', 'UNEXPECTED_MAINTENANCE'
    )),
    billing_period_months INTEGER NOT NULL CHECK (billing_period_months > 0),
    amount NUMERIC(12, 2) NOT NULL CHECK (amount >= 0),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'PAID', 'NOT_RECEIVED')),
    description VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS login (
    role VARCHAR(50),
    password VARCHAR(255)
);
