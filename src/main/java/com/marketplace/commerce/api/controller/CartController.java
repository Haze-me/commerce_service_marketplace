package com.marketplace.commerce.api.controller;

import com.marketplace.commerce.application.dto.AddToCartRequestDto;
import com.marketplace.commerce.application.dto.ApiResponseDto;
import com.marketplace.commerce.application.dto.CartResponseDto;
import com.marketplace.commerce.application.dto.UpdateCartItemRequestDto;
import com.marketplace.commerce.application.service.CartService;
import com.marketplace.commerce.infrastructure.security.CurrentCustomer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Shopping Cart", description = "Authenticated customer's shopping cart")
@RestController
@RequestMapping("/api/v1/commerce/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @Operation(summary = "View the authenticated customer's cart")
    @GetMapping
    public ApiResponseDto<CartResponseDto> getCart(HttpServletRequest request) {
        CartResponseDto cart = cartService.getOrCreateCartView(CurrentCustomer.id());
        return ApiResponseDto.success(cart, "Success", request.getRequestURI());
    }

    @Operation(summary = "Add a product to the cart")
    @PostMapping("/items")
    public ApiResponseDto<CartResponseDto> addItem(
            @Valid @RequestBody AddToCartRequestDto body,
            HttpServletRequest request
    ) {
        CartResponseDto cart = cartService.addItem(CurrentCustomer.id(), body);
        return ApiResponseDto.success(cart, "Item added to cart.", request.getRequestURI());
    }

    @Operation(summary = "Update the quantity of a cart item")
    @PutMapping("/items/{cartItemId}")
    public ApiResponseDto<CartResponseDto> updateItem(
            @PathVariable UUID cartItemId,
            @Valid @RequestBody UpdateCartItemRequestDto body,
            HttpServletRequest request
    ) {
        CartResponseDto cart = cartService.updateItemQuantity(CurrentCustomer.id(), cartItemId, body);
        return ApiResponseDto.success(cart, "Cart item updated.", request.getRequestURI());
    }

    @Operation(summary = "Remove an item from the cart")
    @DeleteMapping("/items/{cartItemId}")
    public ApiResponseDto<CartResponseDto> removeItem(
            @PathVariable UUID cartItemId,
            HttpServletRequest request
    ) {
        CartResponseDto cart = cartService.removeItem(CurrentCustomer.id(), cartItemId);
        return ApiResponseDto.success(cart, "Item removed from cart.", request.getRequestURI());
    }

    @Operation(summary = "Clear all items from the cart")
    @DeleteMapping
    public ApiResponseDto<Void> clearCart(HttpServletRequest request) {
        cartService.clearCart(CurrentCustomer.id());
        return ApiResponseDto.success(null, "Cart cleared.", request.getRequestURI());
    }
}