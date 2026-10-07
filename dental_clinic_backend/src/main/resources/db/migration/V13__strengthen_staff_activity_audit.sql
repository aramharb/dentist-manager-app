ALTER TABLE staff_action
    ALTER COLUMN action_type TYPE VARCHAR(64);

ALTER TABLE staff_action
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN undoable BOOLEAN NOT NULL DEFAULT TRUE;

UPDATE staff_action
SET action_type = CASE
        WHEN resource_type = 'PATIENT' AND action_type = 'CREATE' THEN 'PATIENT_CREATED'
        WHEN resource_type = 'PATIENT' AND action_type = 'UPDATE' THEN 'PATIENT_UPDATED'
        WHEN resource_type = 'APPOINTMENT' AND action_type = 'CREATE' THEN 'APPOINTMENT_CREATED'
        WHEN resource_type = 'APPOINTMENT' AND action_type = 'UPDATE' THEN 'APPOINTMENT_UPDATED'
        WHEN resource_type = 'APPOINTMENT' AND action_type = 'CANCEL' THEN 'APPOINTMENT_CANCELLED'
        WHEN resource_type = 'EXPENSE' AND action_type = 'CREATE' THEN 'EXPENSE_CREATED'
        WHEN resource_type = 'EXPENSE' AND action_type = 'UPDATE' THEN 'EXPENSE_UPDATED'
        WHEN resource_type = 'EXPENSE' AND action_type = 'DELETE' THEN 'EXPENSE_DELETED'
        ELSE action_type
    END,
    status = CASE WHEN undone_at IS NULL THEN 'ACTIVE' ELSE 'UNDONE' END;

UPDATE staff_action
SET undoable = FALSE
WHERE action_type = 'PATIENT_CREATED';

CREATE INDEX idx_staff_action_entity_time
    ON staff_action(resource_type, resource_id, created_at DESC);
