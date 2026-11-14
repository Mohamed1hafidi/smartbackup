ALTER TABLE backups
    ADD COLUMN archive_sha256_hash VARCHAR(64);

UPDATE backups
   SET archive_sha256_hash = sha256_hash
 WHERE archive_sha256_hash IS NULL;

CREATE TABLE restore_jobs (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID         NOT NULL,
    backup_id     UUID         NOT NULL,
    file_id       UUID         NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    ip_address    VARCHAR(45),
    error_message TEXT,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    completed_at  TIMESTAMP WITH TIME ZONE,

    CONSTRAINT fk_restore_jobs_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_restore_jobs_file FOREIGN KEY (file_id)
        REFERENCES files(id) ON DELETE RESTRICT
);

CREATE INDEX idx_restore_jobs_user_id   ON restore_jobs(user_id);
CREATE INDEX idx_restore_jobs_backup_id ON restore_jobs(backup_id);
CREATE INDEX idx_restore_jobs_file_id   ON restore_jobs(file_id);
CREATE INDEX idx_restore_jobs_status    ON restore_jobs(status);
