CREATE TABLE IF NOT EXISTS message_notification (
    id BIGSERIAL PRIMARY KEY,
    message_id BIGINT NOT NULL,
    recipient_id BIGINT NOT NULL,
    conversation_id BIGINT NOT NULL,
    read_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notification_message FOREIGN KEY (message_id) REFERENCES chat_message(id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_notification_recipient FOREIGN KEY (recipient_id) REFERENCES login(id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_notification_conversation FOREIGN KEY (conversation_id) REFERENCES conversation(id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT uq_notification_message_recipient UNIQUE (message_id, recipient_id)
);

INSERT INTO message_notification (message_id, recipient_id, conversation_id, read_at, created_at)
SELECT
    m.id,
    cp.user_id,
    m.conversation_id,
    CASE
        WHEN cp.last_read_at IS NOT NULL AND m.sent_at <= cp.last_read_at THEN cp.last_read_at
        ELSE NULL
    END,
    m.sent_at
FROM chat_message m
JOIN conversation_participant cp ON cp.conversation_id = m.conversation_id
WHERE cp.user_id <> m.sender_id
ON CONFLICT (message_id, recipient_id) DO NOTHING;

CREATE INDEX IF NOT EXISTS idx_notification_recipient_unread
    ON message_notification(recipient_id, read_at)
    WHERE read_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_notification_conversation_recipient
    ON message_notification(conversation_id, recipient_id);
