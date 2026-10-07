ALTER TABLE appointment
    ADD COLUMN end_time TIME;

UPDATE appointment
SET end_time = heure + (duration_minutes * INTERVAL '1 minute')
WHERE end_time IS NULL;

ALTER TABLE appointment
    ALTER COLUMN end_time SET NOT NULL,
    ADD CONSTRAINT chk_appointment_time_range CHECK (end_time > heure),
    ADD CONSTRAINT chk_appointment_duration_positive CHECK (duration_minutes > 0);

CREATE INDEX idx_appointment_provider_schedule
    ON appointment(provider_user_id, date, status, heure, end_time);

CREATE TABLE doctor_working_hours (
    id BIGSERIAL PRIMARY KEY,
    doctor_user_id BIGINT NOT NULL REFERENCES login(id) ON DELETE CASCADE,
    day_of_week INTEGER NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
    working BOOLEAN NOT NULL DEFAULT FALSE,
    start_time TIME,
    end_time TIME,
    CONSTRAINT uk_doctor_working_hours UNIQUE (doctor_user_id, day_of_week),
    CONSTRAINT chk_doctor_working_time CHECK (
        (working = FALSE AND start_time IS NULL AND end_time IS NULL)
        OR (working = TRUE AND start_time IS NOT NULL AND end_time IS NOT NULL AND end_time > start_time)
    )
);

INSERT INTO doctor_working_hours (doctor_user_id, day_of_week, working, start_time, end_time)
SELECT doctor.id, day.day_of_week,
       day.day_of_week BETWEEN 1 AND 5,
       CASE
           WHEN day.day_of_week BETWEEN 1 AND 4 THEN TIME '08:00'
           WHEN day.day_of_week = 5 THEN TIME '08:00'
           ELSE NULL
       END,
       CASE
           WHEN day.day_of_week BETWEEN 1 AND 4 THEN TIME '17:00'
           WHEN day.day_of_week = 5 THEN TIME '15:00'
           ELSE NULL
       END
FROM login doctor
CROSS JOIN generate_series(1, 7) AS day(day_of_week)
WHERE lower(trim(doctor.role)) = 'doctor'
  AND doctor.active = TRUE
ON CONFLICT (doctor_user_id, day_of_week) DO NOTHING;

CREATE INDEX idx_doctor_working_hours_doctor
    ON doctor_working_hours(doctor_user_id, day_of_week);
