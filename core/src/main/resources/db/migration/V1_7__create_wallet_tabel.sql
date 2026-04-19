CREATE TABLE tbl_wallet (
                         id BIGSERIAL PRIMARY KEY,
                         user_id BIGINT NOT NULL UNIQUE,
                         balance DECIMAL(19,2) NOT NULL DEFAULT 0,
                         frozen_balance DECIMAL(19,2) NOT NULL DEFAULT 0,
                         created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         CONSTRAINT fk_wallet_user FOREIGN KEY (user_id) REFERENCES tbl_user(id) ON DELETE CASCADE
);

CREATE INDEX idx_wallet_user_id ON tbl_wallet(user_id);