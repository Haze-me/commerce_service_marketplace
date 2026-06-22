package com.marketplace.commerce.api.controller;

import com.marketplace.commerce.application.dto.ApiResponseDto;
import com.marketplace.commerce.application.dto.CheckoutRequestDto;
import com.marketplace.commerce.application.dto.OrderResponseDto;
import com.marketplace.commerce.application.service.CheckoutService;
import com.marketplace.commerce.infrastructure.security.CurrentCustomer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Orders", description = "Checkout and order management")
@RestController
@RequestMapping("/api/v1/commerce/orders")
@RequiredArgsConstructor
public class OrderController {

    private final CheckoutService checkoutService;

    @Operation(summary = "Checkout the current cart and create an order")
    @PostMapping("/checkout")
    public ApiResponseDto<OrderResponseDto> checkout(
            @Valid @RequestBody CheckoutRequestDto body,
            HttpServletRequest request
    ) {
        OrderResponseDto order = checkoutService.checkout(CurrentCustomer.id(), body);
        return ApiResponseDto.success(order, "Order created successfully.", request.getRequestURI());
    }
}