package com.marketplace.commerce.application.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
public class PaymentInitResponseDto {
    private UUID orderId;
    private String reference;
    private String paymentUrl;
    private BigDecimal amount;
    private String currency;
}