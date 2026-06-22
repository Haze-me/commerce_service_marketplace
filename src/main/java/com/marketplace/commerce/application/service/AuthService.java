package com.marketplace.commerce.application.service;

import com.marketplace.commerce.application.dto.AuthResponseDto;
import com.marketplace.commerce.application.dto.LoginRequestDto;
import com.marketplace.commerce.application.dto.RegisterRequestDto;
import com.marketplace.commerce.domain.model.Customer;
import com.marketplace.commerce.domain.repository.CustomerRepository;
import com.marketplace.commerce.infrastructure.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles customer registration and login.
 * Equivalent to Django's accounts/serializers.py + accounts/views.py combined.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponseDto register(RegisterRequestDto request) {
        if (customerRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new IllegalStateException("A customer with this email already exists.");
        }

        Customer customer = Customer.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail().toLowerCase())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .isActive(true)
                .build();

        customerRepository.save(customer);

        String token = jwtService.generateToken(customer);

        return AuthResponseDto.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .customerId(customer.getId())
                .email(customer.getEmail())
                .fullName(customer.getFullName())
                .build();
    }

    public AuthResponseDto login(LoginRequestDto request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (BadCredentialsException e) {
            throw new BadCredentialsException("Invalid email or password.");
        }

        Customer customer = customerRepository.findByEmailIgnoreCase(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password."));

        String token = jwtService.generateToken(customer);

        return AuthResponseDto.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .customerId(customer.getId())
                .email(customer.getEmail())
                .fullName(customer.getFullName())
                .build();
    }
}