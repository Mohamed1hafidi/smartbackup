CREATE INDEX idx_files_user_id              ON files(user_id);
CREATE INDEX idx_backups_user_id            ON backups(user_id);
CREATE INDEX idx_backup_schedules_user_id   ON backup_schedules(user_id);
CREATE INDEX idx_notifications_user_id      ON notifications(user_id);
CREATE INDEX idx_activity_logs_user_id      ON activity_logs(user_id);

CREATE INDEX idx_backups_file_id            ON backups(file_id);
CREATE INDEX idx_backup_schedules_file_id   ON backup_schedules(file_id);

CREATE INDEX idx_backups_status             ON backups(status);

CREATE INDEX idx_files_created_at           ON files(created_at);
CREATE INDEX idx_backups_created_at         ON backups(created_at);
CREATE INDEX idx_notifications_created_at   ON notifications(created_at);
CREATE INDEX idx_activity_logs_created_at   ON activity_logs(created_at);

CREATE INDEX idx_notifications_is_read      ON notifications(is_read);

CREATE INDEX idx_notifications_user_unread
    ON notifications(user_id, is_read)
    WHERE is_read = FALSE;

CREATE INDEX idx_activity_logs_details
    ON activity_logs USING GIN (details);

CREATE INDEX idx_backup_schedules_active
    ON backup_schedules(is_active)
    WHERE is_active = TRUE;

CREATE INDEX idx_backup_schedules_next_run
    ON backup_schedules(next_run_at)
    WHERE is_active = TRUE;
