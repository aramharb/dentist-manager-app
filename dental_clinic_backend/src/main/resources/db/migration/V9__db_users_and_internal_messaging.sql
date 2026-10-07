ALTER TABLE login
ADD COLUMN IF NOT EXISTS id BIGSERIAL,
ADD COLUMN IF NOT EXISTS username VARCHAR(80),
ADD COLUMN IF NOT EXISTS full_name VARCHAR(160),
ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT TRUE,
ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

UPDATE login
SET username = LOWER(role)
WHERE username IS NULL AND role IS NOT NULL;

UPDATE login
SET role = LOWER(role)
WHERE role IS NOT NULL;

UPDATE login
SET role = CASE
    WHEN role IN ('doctor', 'docteur', 'dentist', 'medecin', 'médecin') THEN 'doctor'
    ELSE 'secretaire'
END;

UPDATE login
SET username = 'user_' || id
WHERE username IS NULL;

UPDATE login
SET full_name = CASE
    WHEN LOWER(role) = 'doctor' THEN 'Dr. Wajih'
    WHEN LOWER(role) = 'secretaire' THEN 'Secretary'
    ELSE INITCAP(COALESCE(role, 'User'))
END
WHERE full_name IS NULL;

UPDATE login
SET password = 'drwajih'
WHERE password IS NULL;

WITH duplicates AS (
    SELECT ctid, username, ROW_NUMBER() OVER (PARTITION BY username ORDER BY id) AS rn
    FROM login
)
UPDATE login l
SET username = duplicates.username || '_' || l.id
FROM duplicates
WHERE l.ctid = duplicates.ctid
  AND duplicates.rn > 1;

ALTER TABLE login
ALTER COLUMN id SET NOT NULL,
ALTER COLUMN username SET NOT NULL,
ALTER COLUMN full_name SET NOT NULL,
ALTER COLUMN role SET NOT NULL,
ALTER COLUMN password SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'login_pkey'
    ) THEN
        ALTER TABLE login ADD CONSTRAINT login_pkey PRIMARY KEY (id);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'login_username_unique'
    ) THEN
        ALTER TABLE login ADD CONSTRAINT login_username_unique UNIQUE (username);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'login_role_check'
    ) THEN
        ALTER TABLE login ADD CONSTRAINT login_role_check CHECK (role IN ('doctor', 'secretaire'));
    END IF;
END $$;

INSERT INTO login (username, full_name, role, password, active)
VALUES
    ('doctor', 'Dr. Wajih', 'doctor', 'drwajih', TRUE),
    ('secretaire', 'Secretary', 'secretaire', 'drwajih', TRUE)
ON CONFLICT (username) DO UPDATE
SET full_name = EXCLUDED.full_name,
    role = EXCLUDED.role,
    active = TRUE,
    updated_at = CURRENT_TIMESTAMP;

CREATE TABLE IF NOT EXISTS conversation (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(180),
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_conversation_created_by FOREIGN KEY (created_by) REFERENCES login(id) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS conversation_participant (
    conversation_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    last_read_at TIMESTAMP,
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (conversation_id, user_id),
    CONSTRAINT fk_cp_conversation FOREIGN KEY (conversation_id) REFERENCES conversation(id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_cp_user FOREIGN KEY (user_id) REFERENCES login(id) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS chat_message (
    id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    body TEXT NOT NULL,
    sent_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_message_conversation FOREIGN KEY (conversation_id) REFERENCES conversation(id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_message_sender FOREIGN KEY (sender_id) REFERENCES login(id) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_conversation_updated_at ON conversation(updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_cp_user ON conversation_participant(user_id);
CREATE INDEX IF NOT EXISTS idx_message_conversation_sent_at ON chat_message(conversation_id, sent_at);
