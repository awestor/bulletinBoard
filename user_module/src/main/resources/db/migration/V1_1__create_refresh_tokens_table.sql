CREATE TABLE tbl_refresh_tokens (
                                    id BIGSERIAL PRIMARY KEY,
                                    token VARCHAR(255) NOT NULL UNIQUE,
                                    user_id BIGINT NOT NULL REFERENCES tbl_user(id) ON DELETE CASCADE,
                                    expires_at TIMESTAMP NOT NULL,
                                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                    revoked_at TIMESTAMP
);

CREATE INDEX idx_refresh_tokens_token ON tbl_refresh_tokens(token);
CREATE INDEX idx_refresh_tokens_user_id ON tbl_refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_expires_at ON tbl_refresh_tokens(expires_at);