-- Cabinet manager: one per cabinet, manages the accounts (doctors and secretaries) of
-- that cabinet only. The platform admin creates it together with the cabinet.
ALTER TABLE login DROP CONSTRAINT IF EXISTS login_role_check;
ALTER TABLE login ADD CONSTRAINT login_role_check
    CHECK (role IN ('doctor', 'secretaire', 'admin', 'manager'));

-- At most one active manager per cabinet.
CREATE UNIQUE INDEX uq_login_active_cabinet_manager
    ON login(cabinet_id) WHERE role = 'manager' AND active = TRUE;

-- One-time links used to choose a first (or a new) password. Only a hash of the token is
-- stored, so a database leak does not expose usable links.
CREATE TABLE user_invitation (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES login(id) ON DELETE CASCADE,
    token_hash CHAR(64) NOT NULL UNIQUE,
    created_by BIGINT REFERENCES login(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP
);
CREATE INDEX idx_user_invitation_user ON user_invitation(user_id);
