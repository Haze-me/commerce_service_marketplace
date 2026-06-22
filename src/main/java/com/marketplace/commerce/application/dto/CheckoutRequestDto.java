package com.marketplace.commerce.application.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CheckoutRequestDto {

    /**
     * Optional — if provided, uses this saved address.
     * If omitted, falls back to the customer's default address.
     */
    private UUID addressId;
}