-- Server-side sessions of staff accounts: every token carries a session id (jti). A session can be
-- revoked (logout, password change, deactivation, too many simultaneous sessions) and expires
-- after a period of inactivity.
CREATE TABLE staff_session (
    id VARCHAR(36) PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES login(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_seen_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP,
    client_address VARCHAR(64)
);
CREATE INDEX idx_staff_session_user_active ON staff_session(user_id) WHERE revoked_at IS NULL;

-- Who looked at or changed client files and clinical records, per cabinet.
CREATE TABLE record_access_log (
    id BIGSERIAL PRIMARY KEY,
    cabinet_id BIGINT NOT NULL REFERENCES cabinet(id),
    user_id BIGINT REFERENCES login(id) ON DELETE SET NULL,
    user_name VARCHAR(160) NOT NULL,
    user_role VARCHAR(20) NOT NULL,
    action VARCHAR(10) NOT NULL CHECK (action IN ('VIEW', 'CREATE', 'UPDATE', 'DELETE')),
    resource VARCHAR(30) NOT NULL,
    resource_id BIGINT,
    client_address VARCHAR(64),
    occurred_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_access_log_cabinet_time ON record_access_log(cabinet_id, occurred_at DESC);
CREATE INDEX idx_access_log_resource ON record_access_log(cabinet_id, resource, resource_id);

-- One client file can be linked to a single online account.
CREATE UNIQUE INDEX uq_membership_linked_patient
    ON client_membership(cabinet_id, patient_id) WHERE patient_id IS NOT NULL;
