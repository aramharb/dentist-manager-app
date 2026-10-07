ALTER TABLE appointment
    ADD COLUMN provider_user_id BIGINT;

UPDATE appointment a
SET provider_user_id = (
    SELECT l.id
    FROM login l
    WHERE l.active = TRUE
      AND lower(trim(l.role)) = 'doctor'
      AND (lower(trim(l.full_name)) = lower(trim(a.provider_name))
        OR lower(trim(l.username)) = lower(trim(a.provider_name)))
    ORDER BY l.id
    LIMIT 1
);

ALTER TABLE appointment
    ADD CONSTRAINT fk_appointment_provider_user
        FOREIGN KEY (provider_user_id) REFERENCES login(id) ON DELETE SET NULL;

CREATE INDEX idx_appointment_provider_user_date
    ON appointment(provider_user_id, date);
