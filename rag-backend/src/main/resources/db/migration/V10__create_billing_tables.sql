ALTER TABLE users ADD COLUMN IF NOT EXISTS points INT NOT NULL DEFAULT 0;

CREATE TABLE payment_orders (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    order_no VARCHAR(64) NOT NULL UNIQUE,
    package_name VARCHAR(100) NOT NULL,
    points INT NOT NULL,
    amount_cents INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payment_channel VARCHAR(30) NOT NULL DEFAULT 'MOCK',
    paid_at TIMESTAMP,
    closed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_payment_orders_user_id ON payment_orders(user_id);
CREATE INDEX idx_payment_orders_status ON payment_orders(status);

CREATE TABLE refund_orders (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    payment_order_id BIGINT NOT NULL REFERENCES payment_orders(id) ON DELETE CASCADE,
    refund_no VARCHAR(64) NOT NULL UNIQUE,
    reason VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reviewer_id BIGINT REFERENCES users(id),
    reviewer_note VARCHAR(500),
    refunded_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_refund_orders_user_id ON refund_orders(user_id);
CREATE INDEX idx_refund_orders_status ON refund_orders(status);

CREATE TABLE point_transactions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(20) NOT NULL,
    change_amount INT NOT NULL,
    balance_after INT NOT NULL,
    source_type VARCHAR(30) NOT NULL,
    source_id BIGINT,
    description VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_point_transactions_user_id ON point_transactions(user_id);
CREATE INDEX idx_point_transactions_created_at ON point_transactions(created_at);
