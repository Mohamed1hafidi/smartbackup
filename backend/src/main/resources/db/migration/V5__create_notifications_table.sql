CREATE TABLE notifications (
    id         UUID                    PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID                    NOT NULL,
    backup_id  UUID,
    type       notification_type_enum  NOT NULL,
    message    TEXT                    NOT NULL,
    is_read    BOOLEAN                 NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_notifications_backup FOREIGN KEY (backup_id)
        REFERENCES backups(id) ON DELETE SET NULL
);
