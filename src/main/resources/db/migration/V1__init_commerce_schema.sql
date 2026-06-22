-- ============================================================
-- V1: Initial commerce schema
-- Commerce Service owns this schema exclusively.
-- ============================================================

CREATE TABLE customers (
                           id UUID PRIMARY KEY,
                           first_name VARCHAR(150) NOT NULL,
                           last_name VARCHAR(150) NOT NULL,
                           email VARCHAR(255) NOT NULL UNIQUE,
                           password VARCHAR(255) NOT NULL,
                           phone VARCHAR(20),
                           is_active BOOLEAN NOT NULL DEFAULT TRUE,
                           created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
                           updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_customers_email ON customers(email);

CREATE TABLE carts (
                       id UUID PRIMARY KEY,
                       customer_id UUID NOT NULL UNIQUE,
                       created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
                       updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

                       CONSTRAINT fk_carts_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE
);

CREATE TABLE cart_items (
                            id UUID PRIMARY KEY,
                            cart_id UUID NOT NULL,
                            product_id UUID NOT NULL,
                            quantity INTEGER NOT NULL CHECK (quantity > 0),
                            created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
                            updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

                            CONSTRAINT fk_cart_items_cart FOREIGN KEY (cart_id) REFERENCES carts(id) ON DELETE CASCADE,
                            CONSTRAINT uq_cart_product UNIQUE (cart_id, product_id)
);

CREATE INDEX idx_cart_items_cart ON cart_items(cart_id);

CREATE TABLE orders (
                        id UUID PRIMARY KEY,
                        customer_id UUID NOT NULL,
                        status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
                        total_amount NUMERIC(12, 2) NOT NULL,
                        shipping_address TEXT NOT NULL,
                        created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
                        updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

                        CONSTRAINT fk_orders_customer FOREIGN KEY (customer_id) REFERENCES customers(id)
);

CREATE INDEX idx_orders_customer ON orders(customer_id);
CREATE INDEX idx_orders_status ON orders(status);

CREATE TABLE order_items (
                             id UUID PRIMARY KEY,
                             order_id UUID NOT NULL,
                             product_id UUID NOT NULL,
                             vendor_id UUID NOT NULL,
                             quantity INTEGER NOT NULL CHECK (quantity > 0),
                             unit_price NUMERIC(12, 2) NOT NULL,
                             created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

                             CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);

CREATE INDEX idx_order_items_order ON order_items(order_id);
CREATE INDEX idx_order_items_vendor ON order_items(vendor_id);

CREATE TABLE payments (
                          id UUID PRIMARY KEY,
                          order_id UUID NOT NULL UNIQUE,
                          status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
                          reference VARCHAR(255) NOT NULL UNIQUE,
                          amount NUMERIC(12, 2) NOT NULL,
                          created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
                          updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

                          CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders(id)
);

CREATE INDEX idx_payments_reference ON payments(reference);

CREATE TABLE reviews (
                         id UUID PRIMARY KEY,
                         customer_id UUID NOT NULL,
                         product_id UUID NOT NULL,
                         rating SMALLINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
                         comment TEXT,
                         created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
                         updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

                         CONSTRAINT fk_reviews_customer FOREIGN KEY (customer_id) REFERENCES customers(id),
                         CONSTRAINT uq_customer_product_review UNIQUE (customer_id, product_id)
);

CREATE INDEX idx_reviews_product ON reviews(product_id);

CREATE TABLE notifications (
                               id UUID PRIMARY KEY,
                               customer_id UUID NOT NULL,
                               message TEXT NOT NULL,
                               read_status BOOLEAN NOT NULL DEFAULT FALSE,
                               created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

                               CONSTRAINT fk_notifications_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE
);

CREATE INDEX idx_notifications_customer ON notifications(customer_id, read_status);