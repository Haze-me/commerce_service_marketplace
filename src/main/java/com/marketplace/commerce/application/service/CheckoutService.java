package com.marketplace.commerce.application.service;

import com.marketplace.commerce.application.dto.CheckoutRequestDto;
import com.marketplace.commerce.application.dto.OrderItemResponseDto;
import com.marketplace.commerce.application.dto.OrderResponseDto;
import com.marketplace.commerce.domain.model.Cart;
import com.marketplace.commerce.domain.model.CartItem;
import com.marketplace.commerce.domain.model.Order;
import com.marketplace.commerce.domain.model.OrderItem;
import com.marketplace.commerce.domain.model.OrderStatus;
import com.marketplace.commerce.domain.repository.CartRepository;
import com.marketplace.commerce.domain.repository.OrderRepository;
import com.marketplace.commerce.infrastructure.client.CatalogServiceClient;
import com.marketplace.commerce.infrastructure.client.InventoryReservationClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Orchestrates the checkout flow:
 * 1. Validate cart is not empty
 * 2. Fetch live product data (price, vendor, stock status) from Catalog Service
 * 3. Reserve stock synchronously via Django (all-or-nothing)
 * 4. Create Order + OrderItems with prices locked at time of purchase
 * 5. Clear the cart
 *
 * If stock reservation fails, NO order is created and the cart is untouched —
 * the customer can adjust quantities and retry.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CheckoutService {

    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final CatalogServiceClient catalogServiceClient;
    private final InventoryReservationClient inventoryReservationClient;
    private final AddressService addressService;

    @Transactional
    public OrderResponseDto checkout(UUID customerId, CheckoutRequestDto request) {
        Cart cart = cartRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new IllegalStateException("Your cart is empty."));

        if (cart.getItems().isEmpty()) {
            throw new IllegalStateException("Your cart is empty.");
        }

        // Step 1: Resolve live product data for every cart item
        record ResolvedItem(UUID productId, UUID vendorId, Integer quantity, BigDecimal unitPrice) {}

        List<ResolvedItem> resolvedItems = cart.getItems().stream()
                .map(cartItem -> {
                    var snapshot = catalogServiceClient.getProductById(cartItem.getProductId())
                            .orElseThrow(() -> new NoSuchElementException(
                                    "Product " + cartItem.getProductId() + " is no longer available."));

                    if (!"ACTIVE".equals(snapshot.status())) {
                        throw new IllegalStateException(
                                "Product '" + snapshot.name() + "' is no longer available for purchase.");
                    }

                    return new ResolvedItem(
                            cartItem.getProductId(),
                            snapshot.vendorId(),
                            cartItem.getQuantity(),
                            snapshot.price()
                    );
                })
                .toList();

        // Step 2: Reserve stock synchronously — all or nothing
        List<InventoryReservationClient.ReservationItem> reservationItems = resolvedItems.stream()
                .map(item -> new InventoryReservationClient.ReservationItem(item.productId(), item.quantity()))
                .toList();

        try {
            inventoryReservationClient.reserveStock(reservationItems);
        } catch (InventoryReservationClient.InsufficientStockException e) {
            throw new IllegalStateException(e.getMessage());
        }

        // Step 3: Build the order — reservation succeeded, safe to commit
        BigDecimal totalAmount = resolvedItems.stream()
                .map(item -> item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String resolvedAddress = addressService.resolveAddressText(customerId, request.getAddressId());

        Order order = Order.builder()
                .customerId(customerId)
                .status(OrderStatus.PAYMENT_PENDING)
                .totalAmount(totalAmount)
                .shippingAddress(resolvedAddress)
                .build();

        List<OrderItem> orderItems = resolvedItems.stream()
                .map(item -> OrderItem.builder()
                        .order(order)
                        .productId(item.productId())
                        .vendorId(item.vendorId())
                        .quantity(item.quantity())
                        .unitPrice(item.unitPrice())
                        .build())
                .toList();

        order.setItems(orderItems);
        orderRepository.save(order);

        // Step 4: Clear the cart now that the order is created
        cart.getItems().clear();
        cartRepository.save(cart);

        log.info("Order {} created for customer {} — status PAYMENT_PENDING, total={}",
                order.getId(), customerId, totalAmount);

        return toDto(order);
    }

    private OrderResponseDto toDto(Order order) {
        List<OrderItemResponseDto> itemDtos = order.getItems().stream()
                .map(item -> OrderItemResponseDto.builder()
                        .productId(item.getProductId())
                        .vendorId(item.getVendorId())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .lineTotal(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                        .build())
                .toList();

        return OrderResponseDto.builder()
                .id(order.getId())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .shippingAddress(order.getShippingAddress())
                .items(itemDtos)
                .createdAt(order.getCreatedAt())
                .build();
    }
}