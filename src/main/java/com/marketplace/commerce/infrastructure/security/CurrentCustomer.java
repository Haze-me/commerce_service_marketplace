package com.marketplace.commerce.infrastructure.security;

import com.marketplace.commerce.domain.model.Customer;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Utility to extract the currently authenticated Customer from
 * Spring Security's context, set there by JwtAuthFilter.
 *
 * Equivalent to Django's request.user inside an authenticated view.
 */
public class CurrentCustomer {

    private CurrentCustomer() {
    }

    public static Customer get() {
        return (Customer) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    public static java.util.UUID id() {
        return get().getId();
    }
}