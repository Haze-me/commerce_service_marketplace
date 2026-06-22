-- ============================================================
-- V2: Customer saved addresses
-- ============================================================

CREATE TABLE customer_addresses (
                                    id UUID PRIMARY KEY,
                                    customer_id UUID NOT NULL,
                                    label VARCHAR(100) NOT NULL,
                                    full_address TEXT NOT NULL,
                                    is_default BOOLEAN NOT NULL DEFAULT FALSE,
                                    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
                                    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

                                    CONSTRAINT fk_customer_addresses_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE
);

CREATE INDEX idx_customer_addresses_customer ON customer_addresses(customer_id);