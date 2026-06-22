package com.marketplace.commerce.application.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
public class PaymentVerifyResponseDto {
    private UUID orderId;
    private String reference;
    private String paymentStatus;
    private String orderStatus;
    private BigDecimal amount;
}