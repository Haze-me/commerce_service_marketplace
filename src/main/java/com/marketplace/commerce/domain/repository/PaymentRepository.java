package com.marketplace.commerce.domain.repository;

import com.marketplace.commerce.domain.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByReference(String reference);
    Optional<Payment> findByOrderId(UUID orderId);
}