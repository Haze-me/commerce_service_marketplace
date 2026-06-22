package com.marketplace.commerce.api.controller;

import com.marketplace.commerce.application.dto.ApiResponseDto;
import com.marketplace.commerce.application.dto.PaymentInitResponseDto;
import com.marketplace.commerce.application.dto.PaymentVerifyResponseDto;
import com.marketplace.commerce.application.service.PaystackService;
import com.marketplace.commerce.infrastructure.security.CurrentCustomer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Payments", description = "Paystack payment initialization and verification")
@RestController
@RequestMapping("/api/v1/commerce/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaystackService paystackService;

    @Operation(summary = "Initialize payment for an order — returns Paystack payment URL")
    @PostMapping("/initialize/{orderId}")
    public ApiResponseDto<PaymentInitResponseDto> initialize(
            @PathVariable UUID orderId,
            HttpServletRequest request
    ) {
        var customer = CurrentCustomer.get();
        PaymentInitResponseDto response = paystackService.initializePayment(
                customer.getId(),
                customer.getEmail(),   // pass real email
                orderId
        );
        return ApiResponseDto.success(response, "Payment initialized.", request.getRequestURI());
    }

    @Operation(summary = "Verify a Paystack payment by reference — marks order PAID on success")
    @PostMapping("/verify/{reference}")
    public ApiResponseDto<PaymentVerifyResponseDto> verify(
            @PathVariable String reference,
            HttpServletRequest request
    ) {
        PaymentVerifyResponseDto response = paystackService.verifyPayment(
                CurrentCustomer.id(),
                reference
        );
        return ApiResponseDto.success(response, "Payment verified.", request.getRequestURI());
    }
}