CREATE TABLE activity_logs (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID         NOT NULL,
    action        VARCHAR(100) NOT NULL,
    resource_type VARCHAR(100),
    resource_id   UUID,
    details       JSONB,
    ip_address    VARCHAR(45),
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_activity_logs_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE RESTRICT
);
