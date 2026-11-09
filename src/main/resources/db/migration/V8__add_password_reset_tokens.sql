CREATE TABLE password_reset_tokens (
    user_id UUID PRIMARY KEY,
    code_hash VARCHAR(100) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_password_reset_tokens_user FOREIGN KEY (user_id) REFERENCES app_users (id) ON DELETE CASCADE,
    CONSTRAINT chk_password_reset_attempts CHECK (attempts >= 0)
);

CREATE INDEX idx_password_reset_tokens_expires_at ON password_reset_tokens (expires_at);
