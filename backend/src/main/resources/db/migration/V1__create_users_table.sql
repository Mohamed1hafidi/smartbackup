CREATE TYPE backup_type_enum AS ENUM ('MANUAL', 'SCHEDULED');
CREATE TYPE backup_status_enum AS ENUM ('PENDING', 'IN_PROGRESS', 'SUCCESS', 'FAILED');
CREATE TYPE frequency_enum AS ENUM ('DAILY', 'WEEKLY');
CREATE TYPE notification_type_enum AS ENUM (
    'BACKUP_SUCCESS',
    'BACKUP_FAILED',
    'RESTORE_SUCCESS',
    'RESTORE_FAILED',
    'SCHEDULE_ACTIVATED',
    'SCHEDULE_DEACTIVATED'
);

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TABLE users (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(320) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name     VARCHAR(255) NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
