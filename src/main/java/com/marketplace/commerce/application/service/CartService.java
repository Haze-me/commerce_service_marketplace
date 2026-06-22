package com.marketplace.commerce.application.service;

import com.marketplace.commerce.application.dto.AddToCartRequestDto;
import com.marketplace.commerce.application.dto.CartItemResponseDto;
import com.marketplace.commerce.application.dto.CartResponseDto;
import com.marketplace.commerce.application.dto.UpdateCartItemRequestDto;
import com.marketplace.commerce.domain.model.Cart;
import com.marketplace.commerce.domain.model.CartItem;
import com.marketplace.commerce.domain.repository.CartItemRepository;
import com.marketplace.commerce.domain.repository.CartRepository;
import com.marketplace.commerce.infrastructure.client.CatalogServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Shopping cart business logic.
 * PostgreSQL (cart_items table) is the source of truth for WHAT is in
 * the cart. Catalog Service is queried live for CURRENT price/name/stock
 * — never cached/stored locally, since prices and stock change frequently
 * and the cart must always reflect reality at checkout time.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CatalogServiceClient catalogServiceClient;

    @Transactional
    public CartResponseDto getOrCreateCartView(UUID customerId) {
        Cart cart = getOrCreateCart(customerId);
        return buildCartResponse(cart);
    }

    @Transactional
    public CartResponseDto addItem(UUID customerId, AddToCartRequestDto request) {
        var productSnapshot = catalogServiceClient.getProductById(request.getProductId())
                .orElseThrow(() -> new NoSuchElementException("Product not found or unavailable."));

        if (!"ACTIVE".equals(productSnapshot.status())) {
            throw new IllegalStateException("This product is not currently available for purchase.");
        }

        Cart cart = getOrCreateCart(customerId);

        CartItem existingItem = cartItemRepository
                .findByCartIdAndProductId(cart.getId(), request.getProductId())
                .orElse(null);

        if (existingItem != null) {
            existingItem.setQuantity(existingItem.getQuantity() + request.getQuantity());
            cartItemRepository.save(existingItem);
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .productId(request.getProductId())
                    .quantity(request.getQuantity())
                    .build();
            cartItemRepository.save(newItem);
        }

        return buildCartResponse(cart);
    }

    @Transactional
    public CartResponseDto updateItemQuantity(UUID customerId, UUID cartItemId, UpdateCartItemRequestDto request) {
        Cart cart = getOrCreateCart(customerId);
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new NoSuchElementException("Cart item not found."));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new NoSuchElementException("Cart item not found.");
        }

        item.setQuantity(request.getQuantity());
        cartItemRepository.save(item);

        return buildCartResponse(cart);
    }

    @Transactional
    public CartResponseDto removeItem(UUID customerId, UUID cartItemId) {
        Cart cart = getOrCreateCart(customerId);
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new NoSuchElementException("Cart item not found."));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new NoSuchElementException("Cart item not found.");
        }

        cartItemRepository.delete(item);
        return buildCartResponse(cart);
    }

    @Transactional
    public void clearCart(UUID customerId) {
        Cart cart = getOrCreateCart(customerId);
        cart.getItems().clear();
        cartRepository.save(cart);
    }

    private Cart getOrCreateCart(UUID customerId) {
        return cartRepository.findByCustomerId(customerId)
                .orElseGet(() -> cartRepository.save(
                        Cart.builder().customerId(customerId).build()
                ));
    }

    private CartResponseDto buildCartResponse(Cart cart) {
        List<CartItemResponseDto> itemDtos = cart.getItems().stream()
                .map(this::toItemDto)
                .filter(java.util.Objects::nonNull)
                .toList();

        BigDecimal subtotal = itemDtos.stream()
                .map(CartItemResponseDto::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalItems = itemDtos.stream()
                .mapToInt(CartItemResponseDto::getQuantity)
                .sum();

        return CartResponseDto.builder()
                .cartId(cart.getId())
                .items(itemDtos)
                .subtotal(subtotal)
                .totalItems(totalItems)
                .build();
    }

    private CartItemResponseDto toItemDto(CartItem item) {
        var snapshotOpt = catalogServiceClient.getProductById(item.getProductId());

        if (snapshotOpt.isEmpty()) {
            log.warn("Product {} in cart could not be resolved from Catalog Service — skipping from view.",
                    item.getProductId());
            return null;
        }

        var snapshot = snapshotOpt.get();
        BigDecimal lineTotal = snapshot.price().multiply(BigDecimal.valueOf(item.getQuantity()));

        return CartItemResponseDto.builder()
                .id(item.getId())
                .productId(item.getProductId())
                .productName(snapshot.name())
                .productImageUrl(snapshot.imageUrl())
                .unitPrice(snapshot.price())
                .quantity(item.getQuantity())
                .lineTotal(lineTotal)
                .inStock(snapshot.inStock())
                .build();
    }
}