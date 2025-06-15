CREATE TABLE products (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    subscription_type VARCHAR(20) NOT NULL CHECK (subscription_type IN ('MONTHLY', 'ANNUAL')),
    price_cents INTEGER NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);