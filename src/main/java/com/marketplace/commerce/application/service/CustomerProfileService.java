package com.marketplace.commerce.application.service;

import com.marketplace.commerce.application.dto.ChangePasswordRequestDto;
import com.marketplace.commerce.application.dto.CustomerProfileDto;
import com.marketplace.commerce.application.dto.UpdateProfileRequestDto;
import com.marketplace.commerce.domain.model.Customer;
import com.marketplace.commerce.domain.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Handles customer's own profile management — view, update, change password.
 * Equivalent to Django's accounts MeView + ChangePasswordView combined.
 */
@Service
@RequiredArgsConstructor
public class CustomerProfileService {

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public CustomerProfileDto getProfile(UUID customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new NoSuchElementException("Customer not found."));
        return toDto(customer);
    }

    @Transactional
    public CustomerProfileDto updateProfile(UUID customerId, UpdateProfileRequestDto request) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new NoSuchElementException("Customer not found."));

        customer.setFirstName(request.getFirstName());
        customer.setLastName(request.getLastName());
        customer.setPhone(request.getPhone());

        customerRepository.save(customer);
        return toDto(customer);
    }

    @Transactional
    public void changePassword(UUID customerId, ChangePasswordRequestDto request) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new NoSuchElementException("Customer not found."));

        if (!passwordEncoder.matches(request.getCurrentPassword(), customer.getPassword())) {
            throw new BadCredentialsException("Current password is incorrect.");
        }

        customer.setPassword(passwordEncoder.encode(request.getNewPassword()));
        customerRepository.save(customer);
    }

    private CustomerProfileDto toDto(Customer customer) {
        return CustomerProfileDto.builder()
                .id(customer.getId())
                .firstName(customer.getFirstName())
                .lastName(customer.getLastName())
                .email(customer.getEmail())
                .phone(customer.getPhone())
                .createdAt(customer.getCreatedAt())
                .build();
    }
}