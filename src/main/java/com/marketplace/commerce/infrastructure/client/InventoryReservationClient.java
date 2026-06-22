package com.marketplace.commerce.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.UUID;

/**
 * Calls the Django Admin & Vendor Service's internal stock reservation
 * endpoints during checkout. This is the second sanctioned synchronous
 * cross-service call in our architecture — overselling prevention requires
 * a confirmed, synchronous stock hold before an order can be created.
 *
 * This is NEVER called directly by a customer, vendor, or admin via
 * Postman/Swagger in normal use — it is called internally, in the
 * background, by CheckoutService the moment a customer hits checkout.
 */
@Slf4j
@Component
public class InventoryReservationClient {

    private final RestClient restClient;
    private final ObjectMapper debugMapper = new ObjectMapper();

    public InventoryReservationClient(@Value("${admin-service.base-url}") String baseUrl) {
        org.apache.hc.client5.http.impl.classic.CloseableHttpClient httpClient =
                org.apache.hc.client5.http.impl.classic.HttpClients.createDefault();

        org.springframework.http.client.HttpComponentsClientHttpRequestFactory factory =
                new org.springframework.http.client.HttpComponentsClientHttpRequestFactory(httpClient);

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }

    /**
     * Reserves stock for all items. All-or-nothing on the Django side.
     * Throws InsufficientStockException if any item cannot be reserved.
     */
    public void reserveStock(List<ReservationItem> items) {
        ReservationRequest req = new ReservationRequest(items);
        logOutgoingPayload("reserve", req);

        try {
            restClient.post()
                    .uri("/api/v1/inventory/internal/reserve/")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(req)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 409) {
                throw new InsufficientStockException("One or more items are out of stock.");
            }
            log.error("Stock reservation failed: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Failed to reserve stock: " + e.getMessage());
        }
    }

    public void releaseStock(List<ReservationItem> items) {
        ReservationRequest req = new ReservationRequest(items);
        logOutgoingPayload("release", req);

        try {
            restClient.post()
                    .uri("/api/v1/inventory/internal/release/")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(req)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            log.error("Stock release failed (order will remain in inconsistent state, needs manual review): {} - {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
        }
    }

    public void confirmSale(List<ReservationItem> items) {
        ReservationRequest req = new ReservationRequest(items);
        logOutgoingPayload("confirm", req);

        try {
            restClient.post()
                    .uri("/api/v1/inventory/internal/confirm/")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(req)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            log.error("Sale confirmation failed: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Failed to confirm sale: " + e.getMessage());
        }
    }

    private void logOutgoingPayload(String operation, ReservationRequest req) {
        try {
            log.info("DEBUG [{}] - JSON being sent: {}", operation, debugMapper.writeValueAsString(req));
        } catch (Exception jsonEx) {
            log.error("DEBUG [{}] - Failed to serialize for logging", operation, jsonEx);
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReservationItem {
        @JsonProperty("product_id")
        private UUID productId;
        @JsonProperty("quantity")
        private Integer quantity;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReservationRequest {
        private List<ReservationItem> items;
    }

    public static class InsufficientStockException extends RuntimeException {
        public InsufficientStockException(String message) {
            super(message);
        }
    }
}