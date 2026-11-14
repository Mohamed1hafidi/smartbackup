CREATE TABLE files (
    id            UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID          NOT NULL,
    original_name VARCHAR(500)  NOT NULL,
    stored_key    VARCHAR(1000) NOT NULL,
    file_size     BIGINT        NOT NULL CHECK (file_size >= 0),
    mime_type     VARCHAR(255)  NOT NULL,
    sha256_hash   VARCHAR(64)   NOT NULL,
    is_deleted    BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_files_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE
);

CREATE TRIGGER trg_files_updated_at
    BEFORE UPDATE ON files
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
