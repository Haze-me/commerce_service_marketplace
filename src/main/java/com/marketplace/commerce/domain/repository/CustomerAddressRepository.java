package com.marketplace.commerce.domain.repository;

import com.marketplace.commerce.domain.model.CustomerAddress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerAddressRepository extends JpaRepository<CustomerAddress, UUID> {
    List<CustomerAddress> findByCustomerId(UUID customerId);
    Optional<CustomerAddress> findByIdAndCustomerId(UUID id, UUID customerId);
    Optional<CustomerAddress> findByCustomerIdAndIsDefaultTrue(UUID customerId);
}