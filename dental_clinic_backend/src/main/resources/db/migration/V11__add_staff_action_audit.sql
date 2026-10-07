CREATE TABLE staff_action (
    id BIGSERIAL PRIMARY KEY,
    actor_user_id BIGINT NOT NULL REFERENCES login(id),
    action_type VARCHAR(20) NOT NULL,
    resource_type VARCHAR(30) NOT NULL,
    resource_id BIGINT NOT NULL,
    description VARCHAR(500) NOT NULL,
    before_state TEXT,
    after_state TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    undone_at TIMESTAMP,
    undone_by_user_id BIGINT REFERENCES login(id)
);

CREATE INDEX idx_staff_action_created_at ON staff_action(created_at DESC);
CREATE INDEX idx_staff_action_pending ON staff_action(undone_at) WHERE undone_at IS NULL;
