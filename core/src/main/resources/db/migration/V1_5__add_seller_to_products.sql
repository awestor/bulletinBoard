ALTER TABLE tbl_products
    ADD COLUMN seller_id BIGINT NOT NULL DEFAULT 1;

ALTER TABLE tbl_products
    ADD CONSTRAINT fk_product_seller
        FOREIGN KEY (seller_id) REFERENCES tbl_user(id) ON DELETE RESTRICT;

-- Индекс для оптимизации запросов
CREATE INDEX idx_product_seller_id ON tbl_products(seller_id);