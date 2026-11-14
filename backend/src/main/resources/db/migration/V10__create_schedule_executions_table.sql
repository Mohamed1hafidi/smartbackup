ALTER TABLE backup_schedules
    ADD COLUMN consecutive_failures INTEGER NOT NULL DEFAULT 0;

CREATE TABLE schedule_executions (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    schedule_id   UUID         NOT NULL,
    backup_id     UUID,
    status        VARCHAR(20)  NOT NULL,
    error_message TEXT,
    duration_ms   BIGINT,
    started_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at  TIMESTAMP WITH TIME ZONE,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_schedule_executions_schedule FOREIGN KEY (schedule_id)
        REFERENCES backup_schedules(id) ON DELETE CASCADE,
    CONSTRAINT fk_schedule_executions_backup FOREIGN KEY (backup_id)
        REFERENCES backups(id) ON DELETE SET NULL
);

CREATE INDEX idx_schedule_executions_schedule_id ON schedule_executions(schedule_id);
CREATE INDEX idx_schedule_executions_backup_id   ON schedule_executions(backup_id);
CREATE INDEX idx_schedule_executions_status      ON schedule_executions(status);
CREATE INDEX idx_schedule_executions_created_at  ON schedule_executions(created_at);
