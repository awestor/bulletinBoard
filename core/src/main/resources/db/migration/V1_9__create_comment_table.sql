CREATE TABLE IF NOT EXISTS tbl_comment (
                                           id BIGSERIAL PRIMARY KEY,
                                           content TEXT NOT NULL,
                                           rating INTEGER NOT NULL DEFAULT 0,
                                           product_id BIGINT NOT NULL,
                                           user_id BIGINT NOT NULL,
                                           created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                           updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                           CONSTRAINT fk_comment_product FOREIGN KEY (product_id)
    REFERENCES tbl_products(id) ON DELETE CASCADE,
    CONSTRAINT fk_comment_user FOREIGN KEY (user_id)
    REFERENCES tbl_user(id) ON DELETE CASCADE,

    -- Ограничение на рейтинг (например, от 1 до 5)
    CONSTRAINT chk_comment_rating CHECK (rating >= 1 AND rating <= 5)
);

CREATE INDEX idx_comment_product_id ON tbl_comment(product_id);