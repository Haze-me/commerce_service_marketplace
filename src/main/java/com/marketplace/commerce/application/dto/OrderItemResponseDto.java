package com.marketplace.commerce.application.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
public class OrderItemResponseDto {
    private UUID productId;
    private UUID vendorId;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;
}