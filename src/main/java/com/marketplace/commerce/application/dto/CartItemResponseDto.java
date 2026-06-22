package com.marketplace.commerce.application.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A single cart line item, enriched with live product data
 * fetched from the Catalog Service at read time.
 */
@Getter
@Builder
public class CartItemResponseDto {
    private UUID id;
    private UUID productId;
    private String productName;
    private String productImageUrl;
    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal lineTotal;
    private Boolean inStock;
}