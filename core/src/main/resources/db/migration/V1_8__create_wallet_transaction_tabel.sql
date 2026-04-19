CREATE TABLE tbl_wallet_transaction (
    id BIGSERIAL PRIMARY KEY,
    wallet_id BIGINT NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    transaction_type VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    description TEXT,
    order_id BIGINT,
    external_deposit VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_transaction_wallet FOREIGN KEY (wallet_id) REFERENCES tbl_wallet(id) ON DELETE CASCADE,

    -- Ограничение 1: ровно одна ссылка (либо order_id, либо external_deposit)
    CONSTRAINT chk_transaction_reference CHECK (
        (order_id IS NOT NULL AND external_deposit IS NULL) OR
        (order_id IS NULL AND external_deposit IS NOT NULL)
        )
);

-- Ограничение 2: уникальность комбинации (transaction_type, status != 'FAILED', order_id, external_deposit)
CREATE UNIQUE INDEX uk_transaction_unique_active
    ON tbl_wallet_transaction (order_id, external_deposit, transaction_type)
    WHERE status != 'FAILED';

CREATE INDEX idx_transaction_wallet_id ON tbl_wallet_transaction(wallet_id);