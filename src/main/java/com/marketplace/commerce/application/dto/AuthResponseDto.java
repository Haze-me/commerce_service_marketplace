package com.marketplace.commerce.application.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class AuthResponseDto {
    private String accessToken;
    private String tokenType;
    private UUID customerId;
    private String email;
    private String fullName;
}