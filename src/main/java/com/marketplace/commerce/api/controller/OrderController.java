package com.marketplace.commerce.api.controller;

import com.marketplace.commerce.application.dto.ApiResponseDto;
import com.marketplace.commerce.application.dto.CheckoutRequestDto;
import com.marketplace.commerce.application.dto.OrderResponseDto;
import com.marketplace.commerce.application.service.CheckoutService;
import com.marketplace.commerce.application.service.OrderService;
import com.marketplace.commerce.infrastructure.security.CurrentCustomer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Orders", description = "Checkout and order management")
@RestController
@RequestMapping("/api/v1/commerce/orders")
@RequiredArgsConstructor
public class OrderController {

    private final CheckoutService checkoutService;
    private final OrderService orderService;

    @Operation(summary = "Checkout the current cart and create an order")
    @PostMapping("/checkout")
    public ApiResponseDto<OrderResponseDto> checkout(
            @Valid @RequestBody CheckoutRequestDto body,
            HttpServletRequest request
    ) {
        OrderResponseDto order = checkoutService.checkout(CurrentCustomer.id(), body);
        return ApiResponseDto.success(order, "Order created successfully.", request.getRequestURI());
    }

    @Operation(summary = "List all orders for the authenticated customer")
    @GetMapping
    public ApiResponseDto<Page<OrderResponseDto>> listOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request
    ) {
        Page<OrderResponseDto> orders = orderService.listOrders(CurrentCustomer.id(), page, size);
        return ApiResponseDto.success(orders, "Success", request.getRequestURI());
    }

    @Operation(summary = "Get a single order by ID")
    @GetMapping("/{orderId}")
    public ApiResponseDto<OrderResponseDto> getOrder(
            @PathVariable UUID orderId,
            HttpServletRequest request
    ) {
        OrderResponseDto order = orderService.getOrder(CurrentCustomer.id(), orderId);
        return ApiResponseDto.success(order, "Success", request.getRequestURI());
    }
}