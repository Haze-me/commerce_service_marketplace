package com.marketplace.commerce.domain.model;

/**
 * Order status constants. Stored as plain strings in the database
 * (not a JPA @Enumerated type) for flexibility — matches Flyway schema's
 * VARCHAR(30) status column.
 */
public class OrderStatus {
    public static final String PENDING = "PENDING";
    public static final String PAYMENT_PENDING = "PAYMENT_PENDING";
    public static final String PAID = "PAID";
    public static final String COMPLETED = "COMPLETED";
    public static final String CANCELLED = "CANCELLED";
    public static final String FAILED = "FAILED";

    private OrderStatus() {
    }
}