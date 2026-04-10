-- Создание таблицы ролей
CREATE TABLE IF NOT EXISTS tbl_roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description TEXT
    );

-- Создание таблицы пользователей
CREATE TABLE IF NOT EXISTS tbl_user (
    id BIGSERIAL PRIMARY KEY,
    login VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    image_name VARCHAR(255),
    blocked_until DATE,
    trading_blocked BOOLEAN NOT NULL DEFAULT FALSE
    );

-- Создание таблицы связей пользователей и ролей
CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL REFERENCES tbl_user(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES tbl_roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
    );

-- Создание таблицы категорий
CREATE TABLE IF NOT EXISTS tbl_categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    type VARCHAR(50) NOT NULL,
    parent_id BIGINT REFERENCES tbl_categories(id) ON DELETE SET NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- Создание таблицы скидок
CREATE TABLE IF NOT EXISTS tbl_discounts (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255),
    description TEXT,
    percentage DECIMAL(5, 2),
    fixed_amount DECIMAL(10, 2),
    applicable_category_id BIGINT,
    start_date TIMESTAMP,
    end_date TIMESTAMP,
    usage_limit INTEGER,
    usage_count INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    type VARCHAR(50)
    );

-- Создание таблицы товаров
CREATE TABLE IF NOT EXISTS tbl_products (
    id BIGSERIAL PRIMARY KEY,
    category_id BIGINT NOT NULL REFERENCES tbl_categories(id) ON DELETE RESTRICT,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10, 2) NOT NULL,
    price_at_time DECIMAL(10, 2) NOT NULL,
    discount_id BIGINT REFERENCES tbl_discounts(id) ON DELETE SET NULL,
    sku VARCHAR(100) UNIQUE,
    stock_quantity INTEGER,
    status VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- Создание таблицы изображений товаров
CREATE TABLE IF NOT EXISTS tbl_product_image (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES tbl_products(id) ON DELETE CASCADE,
    path VARCHAR(500) NOT NULL,
    is_main BOOLEAN DEFAULT FALSE
    );

-- Создание таблицы атрибутов товаров
CREATE TABLE IF NOT EXISTS tbl_product_attributes (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES tbl_products(id) ON DELETE CASCADE,
    key VARCHAR(255) NOT NULL,
    value TEXT NOT NULL
    );

-- Создание таблицы заказов
CREATE TABLE IF NOT EXISTS tbl_orders (
    id BIGSERIAL PRIMARY KEY,
    order_number VARCHAR(50) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL REFERENCES tbl_user(id) ON DELETE RESTRICT,
    status VARCHAR(50) NOT NULL,
    total_price DECIMAL(10, 2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
    );

-- Создание таблицы позиций заказа
CREATE TABLE IF NOT EXISTS tbl_order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES tbl_orders(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES tbl_products(id) ON DELETE RESTRICT,
    quantity INTEGER NOT NULL,
    price_at_time DECIMAL(10, 2) NOT NULL,
    reserved_until TIMESTAMP NOT NULL,
    UNIQUE (product_id, order_id)
    );

-- Создание таблицы скидок на заказы
CREATE TABLE IF NOT EXISTS tbl_order_discounts (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES tbl_orders(id) ON DELETE CASCADE,
    discount_id BIGINT NOT NULL REFERENCES tbl_discounts(id) ON DELETE RESTRICT,
    discount_amount DECIMAL(10, 2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    reserved_until TIMESTAMP NOT NULL,
    UNIQUE (order_id, discount_id)
    );

-- Создание таблицы способов оплаты
CREATE TABLE IF NOT EXISTS tbl_payment_method (
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- Создание таблицы информации об оплате
CREATE TABLE IF NOT EXISTS tbl_payment_info (
    id BIGSERIAL PRIMARY KEY,
    order_number VARCHAR(50) NOT NULL UNIQUE,
    payment_method_id BIGINT REFERENCES tbl_payment_method(id) ON DELETE SET NULL,
    total_price DECIMAL(10, 2) NOT NULL,
    status VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- Создание индексов для оптимизации запросов
CREATE INDEX idx_products_category ON tbl_products(category_id);
CREATE INDEX idx_products_discount ON tbl_products(discount_id);
CREATE INDEX idx_orders_user ON tbl_orders(user_id);
CREATE INDEX idx_orders_status ON tbl_orders(status);
CREATE INDEX idx_order_items_order ON tbl_order_items(order_id);
CREATE INDEX idx_order_items_product ON tbl_order_items(product_id);
CREATE INDEX idx_order_discounts_order ON tbl_order_discounts(order_id);
CREATE INDEX idx_order_discounts_discount ON tbl_order_discounts(discount_id);
CREATE INDEX idx_payment_info_order ON tbl_payment_info(order_number);
CREATE INDEX idx_categories_parent ON tbl_categories(parent_id);
CREATE INDEX idx_product_attributes_product ON tbl_product_attributes(product_id);
CREATE INDEX idx_product_images_product ON tbl_product_image(product_id);
CREATE INDEX idx_user_roles_user ON user_roles(user_id);