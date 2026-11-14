CREATE TABLE backup_schedules (
    id             UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID            NOT NULL,
    file_id        UUID            NOT NULL,
    frequency      frequency_enum  NOT NULL,
    scheduled_time TIME            NOT NULL,
    day_of_week    VARCHAR(10),
    timezone       VARCHAR(100)    NOT NULL DEFAULT 'UTC',
    is_active      BOOLEAN         NOT NULL DEFAULT TRUE,
    last_run_at    TIMESTAMP WITH TIME ZONE,
    next_run_at    TIMESTAMP WITH TIME ZONE,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_schedules_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_schedules_file FOREIGN KEY (file_id)
        REFERENCES files(id) ON DELETE CASCADE
);

CREATE TRIGGER trg_backup_schedules_updated_at
    BEFORE UPDATE ON backup_schedules
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
