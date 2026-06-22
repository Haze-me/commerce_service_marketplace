package com.marketplace.commerce.application.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class CartResponseDto {
    private UUID cartId;
    private List<CartItemResponseDto> items;
    private BigDecimal subtotal;
    private Integer totalItems;
}