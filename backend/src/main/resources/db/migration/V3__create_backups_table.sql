CREATE TABLE backups (
    id             UUID                PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID                NOT NULL,
    file_id        UUID                NOT NULL,
    r2_object_key  VARCHAR(1000)       NOT NULL UNIQUE,
    r2_bucket      VARCHAR(255)        NOT NULL,
    file_size      BIGINT              NOT NULL CHECK (file_size >= 0),
    sha256_hash    VARCHAR(64)         NOT NULL,
    backup_type    backup_type_enum    NOT NULL,
    status         backup_status_enum  NOT NULL DEFAULT 'PENDING',
    error_message  TEXT,
    version_number INTEGER             NOT NULL DEFAULT 1 CHECK (version_number >= 1),
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    completed_at   TIMESTAMP WITH TIME ZONE,

    CONSTRAINT fk_backups_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_backups_file FOREIGN KEY (file_id)
        REFERENCES files(id) ON DELETE RESTRICT
);
