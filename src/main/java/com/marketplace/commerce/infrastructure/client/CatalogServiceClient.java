package com.marketplace.commerce.infrastructure.client;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * HTTP client for read-only calls to the Catalog Service.
 *
 * This is the ONE sanctioned synchronous service-to-service call in our
 * architecture: Commerce Service needs live product price/name/stock
 * when building a cart, which only Catalog Service knows.
 *
 * Fails gracefully — if Catalog Service is unreachable, returns
 * Optional.empty() rather than crashing the cart operation.
 */
@Slf4j
@Component
public class CatalogServiceClient {

    private final RestClient restClient;

    public CatalogServiceClient(@Value("${catalog-service.base-url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public Optional<ProductSnapshot> getProductById(UUID productId) {
        try {
            CatalogApiResponse response = restClient.get()
                    .uri("/api/v1/catalog/products/by-id/{id}", productId)
                    .retrieve()
                    .body(CatalogApiResponse.class);

            if (response == null || response.data == null) {
                return Optional.empty();
            }

            return Optional.of(new ProductSnapshot(
                    response.data.productId,
                    response.data.vendorId,
                    response.data.name,
                    response.data.price,
                    response.data.primaryImageUrl,
                    response.data.inStock,
                    response.data.status
            ));

        } catch (RestClientException e) {
            log.error("Failed to fetch product {} from Catalog Service: {}", productId, e.getMessage());
            return Optional.empty();
        }
    }

    // ---- Internal response shape matching Catalog Service's ApiResponseDto<ProductDetailDto> ----

    @Getter
    @Setter
    private static class CatalogApiResponse {
        private boolean success;
        private ProductData data;
    }

    @Getter
    @Setter
    private static class ProductData {
        private UUID productId;
        private UUID vendorId;
        private String name;
        private BigDecimal price;
        private String primaryImageUrl;
        private Boolean inStock;
        private String status;
    }

    public record ProductSnapshot(
            UUID productId,
            UUID vendorId,
            String name,
            BigDecimal price,
            String imageUrl,
            Boolean inStock,
            String status
    ) {
    }
}