package com.marketplace.commerce.application.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class AddressResponseDto {
    private UUID id;
    private String label;
    private String fullAddress;
    private Boolean isDefault;
}