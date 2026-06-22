package com.marketplace.commerce.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddressRequestDto {

    @NotBlank(message = "Label is required (e.g. Home, Office)")
    private String label;

    @NotBlank(message = "Full address is required")
    private String fullAddress;

    private Boolean isDefault;
}