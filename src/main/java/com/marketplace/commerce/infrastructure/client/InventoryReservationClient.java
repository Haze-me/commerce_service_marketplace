package com.marketplace.commerce.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class InventoryReservationClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public InventoryReservationClient(
            @Value("${admin-service.base-url}") String baseUrl,
            ObjectMapper objectMapper
    ) {
        this.objectMapper = objectMapper;

        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpComponentsClientHttpRequestFactory factory =
                new HttpComponentsClientHttpRequestFactory(httpClient);

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();

        log.info("[InventoryReservationClient] Initialized. Base URL: {}", baseUrl);
    }

    public void reserveStock(List<ReservationItem> items) {
        String json = serialize(new ReservationRequest(items));
        log.info("[InventoryClient][RESERVE] Sending: {}", json);
        post("/api/v1/inventory/internal/reserve/", json, true);
    }

    public void releaseStock(List<ReservationItem> items) {
        String json = serialize(new ReservationRequest(items));
        log.info("[InventoryClient][RELEASE] Sending: {}", json);
        post("/api/v1/inventory/internal/release/", json, false);
    }

    public void confirmSale(List<ReservationItem> items) {
        String json = serialize(new ReservationRequest(items));
        log.info("[InventoryClient][CONFIRM] Sending: {}", json);
        post("/api/v1/inventory/internal/confirm/", json, true);
    }

    // ── Private helpers ──────────────────────────────────────────────────

    /**
     * Sends a pre-serialized JSON string as the request body.
     * Using String body bypasses Spring's HttpMessageConverter pipeline entirely
     * — what you see in the log is exactly what Django receives.
     */
    private void post(String uri, String jsonBody, boolean throwOn409) {
        try {
            restClient.post()
                    .uri(uri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jsonBody)   // <-- String, not an object
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            String responseBody = e.getResponseBodyAsString();
            log.error("[InventoryClient] HTTP {} from Django: {}", status, responseBody);

            if (throwOn409 && status == 409) {
                throw new InsufficientStockException("One or more items are out of stock.");
            }
            if (throwOn409) {
                throw new RuntimeException(
                        "Inventory call failed [" + status + "]: " + responseBody
                );
            }
            // For release: log and swallow — don't crash the cancel flow
        }
    }

    private String serialize(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize inventory request", e);
        }
    }

    // ── DTOs ─────────────────────────────────────────────────────────────

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