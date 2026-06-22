package com.marketplace.commerce.application.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketplace.commerce.application.dto.PaymentInitResponseDto;
import com.marketplace.commerce.application.dto.PaymentVerifyResponseDto;
import com.marketplace.commerce.domain.model.*;
import com.marketplace.commerce.domain.repository.OrderRepository;
import com.marketplace.commerce.domain.repository.PaymentRepository;
import com.marketplace.commerce.infrastructure.client.InventoryReservationClient;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Service
public class PaystackService {

    private final RestClient paystackClient;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final InventoryReservationClient inventoryReservationClient;
    private final ObjectMapper objectMapper;

    public PaystackService(
            @Value("${paystack.secret-key}") String secretKey,
            @Value("${paystack.base-url}") String baseUrl,
            OrderRepository orderRepository,
            PaymentRepository paymentRepository,
            InventoryReservationClient inventoryReservationClient,
            ObjectMapper objectMapper
    ) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.inventoryReservationClient = inventoryReservationClient;
        this.objectMapper = objectMapper;

        this.paystackClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + secretKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────
    // INITIALIZE PAYMENT
    // ─────────────────────────────────────────────────────────────────────

    @Transactional
    public PaymentInitResponseDto initializePayment(UUID customerId, String customerEmail, UUID orderId) {
        // 1. Fetch and validate order
        Order order = orderRepository.findByIdAndCustomerId(orderId, customerId)
                .orElseThrow(() -> new NoSuchElementException("Order not found."));

        if (!OrderStatus.PAYMENT_PENDING.equals(order.getStatus())) {
            throw new IllegalStateException(
                    "Order is not awaiting payment. Current status: " + order.getStatus());
        }

        // 2. Check if a payment record already exists (idempotency)
        Optional<Payment> existingPayment = paymentRepository.findByOrderId(orderId);
        if (existingPayment.isPresent()
                && PaymentStatus.SUCCESS.equals(existingPayment.get().getStatus())) {
            throw new IllegalStateException("This order has already been paid.");
        }

        // 3. Generate a unique reference
        String reference = "MKT-" + orderId.toString().replace("-", "").substring(0, 12).toUpperCase()
                + "-" + System.currentTimeMillis();

        // 4. Paystack expects amount in KOBO (smallest currency unit) — multiply by 100
        long amountInKobo = order.getTotalAmount()
                .multiply(BigDecimal.valueOf(100))
                .longValue();

        // 5. Call Paystack initialize endpoint
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("email", customerEmail);
        requestBody.put("amount", amountInKobo);
        requestBody.put("reference", reference);
        requestBody.put("currency", "NGN");

        String jsonBody;
        try {
            jsonBody = objectMapper.writeValueAsString(requestBody);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize Paystack request", e);
        }

        log.info("[Paystack] Initializing payment for order {} — reference: {}", orderId, reference);

        PaystackInitResponse paystackResponse = paystackClient.post()
                .uri("/transaction/initialize")
                .body(jsonBody)
                .retrieve()
                .body(PaystackInitResponse.class);

        if (paystackResponse == null || !paystackResponse.status
                || paystackResponse.data == null) {
            throw new RuntimeException("Paystack initialization failed.");
        }

        // 6. Persist the payment record
        Payment payment = existingPayment.orElseGet(() -> Payment.builder()
                .orderId(orderId)
                .amount(order.getTotalAmount())
                .build());

        payment.setReference(reference);
        payment.setStatus(PaymentStatus.PENDING);
        paymentRepository.save(payment);

        log.info("[Paystack] Payment initialized. URL: {}", paystackResponse.data.authorizationUrl);

        return PaymentInitResponseDto.builder()
                .orderId(orderId)
                .reference(reference)
                .paymentUrl(paystackResponse.data.authorizationUrl)
                .amount(order.getTotalAmount())
                .currency("NGN")
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────
    // VERIFY PAYMENT
    // ─────────────────────────────────────────────────────────────────────

    @Transactional
    public PaymentVerifyResponseDto verifyPayment(UUID customerId, String reference) {
        // 1. Find the local payment record
        Payment payment = paymentRepository.findByReference(reference)
                .orElseThrow(() -> new NoSuchElementException("Payment record not found."));

        // 2. Find the order and verify it belongs to this customer
        Order order = orderRepository.findByIdAndCustomerId(payment.getOrderId(), customerId)
                .orElseThrow(() -> new NoSuchElementException("Order not found."));

        // 3. Idempotency — if already verified, just return current state
        if (PaymentStatus.SUCCESS.equals(payment.getStatus())) {
            log.info("[Paystack] Payment {} already verified — returning cached result.", reference);
            return buildVerifyResponse(payment, order);
        }

        // 4. Call Paystack to verify
        log.info("[Paystack] Verifying payment reference: {}", reference);

        PaystackVerifyResponse paystackResponse = paystackClient.get()
                .uri("/transaction/verify/{reference}", reference)
                .retrieve()
                .body(PaystackVerifyResponse.class);

        if (paystackResponse == null || !paystackResponse.status
                || paystackResponse.data == null) {
            throw new RuntimeException("Paystack verification call failed.");
        }

        String paystackStatus = paystackResponse.data.status; // "success", "failed", "abandoned"
        log.info("[Paystack] Verification result for {}: {}", reference, paystackStatus);

        if ("success".equals(paystackStatus)) {
            // 5a. Payment succeeded — mark payment and order
            payment.setStatus(PaymentStatus.SUCCESS);
            order.setStatus(OrderStatus.PAID);
            paymentRepository.save(payment);
            orderRepository.save(order);

            // 5b. Confirm stock sale with Django (reserved → sold)
            confirmStockSale(order);

            log.info("[Paystack] Order {} marked PAID. Stock confirmed.", order.getId());

        } else {
            // 5c. Payment failed or abandoned
            payment.setStatus(PaymentStatus.FAILED);
            order.setStatus(OrderStatus.FAILED);
            paymentRepository.save(payment);
            orderRepository.save(order);

            // 5d. Release reserved stock back to available
            releaseStock(order);

            log.info("[Paystack] Payment {} failed/abandoned. Stock released.", reference);
        }

        return buildVerifyResponse(payment, order);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────

    private void confirmStockSale(Order order) {
        try {
            List<InventoryReservationClient.ReservationItem> items = order.getItems().stream()
                    .map(i -> new InventoryReservationClient.ReservationItem(
                            i.getProductId(), i.getQuantity()))
                    .toList();
            inventoryReservationClient.confirmSale(items);
        } catch (Exception e) {
            // Log but don't fail — order is already marked PAID.
            // A background job / manual reconciliation can fix stock later.
            log.error("[Paystack] Failed to confirm stock sale for order {}: {}",
                    order.getId(), e.getMessage());
        }
    }

    private void releaseStock(Order order) {
        try {
            List<InventoryReservationClient.ReservationItem> items = order.getItems().stream()
                    .map(i -> new InventoryReservationClient.ReservationItem(
                            i.getProductId(), i.getQuantity()))
                    .toList();
            inventoryReservationClient.releaseStock(items);
        } catch (Exception e) {
            log.error("[Paystack] Failed to release stock for order {}: {}",
                    order.getId(), e.getMessage());
        }
    }

//    private String getCustomerEmail(UUID customerId) {
//        // We don't have Customer injected here to keep the service focused.
//        // The email is embedded in the JWT but we don't need it — Paystack
//        // just needs A valid email. We reconstruct it from the security context
//        // via a simple workaround: pass it from the controller.
//        // For now return a placeholder — fixed in controller below.
//        return "customer@marketplace.com";
//    }

    private PaymentVerifyResponseDto buildVerifyResponse(Payment payment, Order order) {
        return PaymentVerifyResponseDto.builder()
                .orderId(order.getId())
                .reference(payment.getReference())
                .paymentStatus(payment.getStatus())
                .orderStatus(order.getStatus())
                .amount(payment.getAmount())
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────
    // Paystack API response shapes
    // ─────────────────────────────────────────────────────────────────────

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class PaystackInitResponse {
        private boolean status;
        private String message;
        private InitData data;

        @Getter @Setter @NoArgsConstructor @JsonIgnoreProperties(ignoreUnknown = true)
        static class InitData {
            @JsonProperty("authorization_url")
            private String authorizationUrl;
            private String reference;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class PaystackVerifyResponse {
        private boolean status;
        private String message;
        private VerifyData data;

        @Getter @Setter @NoArgsConstructor @JsonIgnoreProperties(ignoreUnknown = true)
        static class VerifyData {
            private String status;   // "success" | "failed" | "abandoned"
            private String reference;
            private Long amount;     // in kobo
        }
    }
}