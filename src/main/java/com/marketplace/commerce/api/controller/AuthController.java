package com.marketplace.commerce.api.controller;

import com.marketplace.commerce.application.dto.ApiResponseDto;
import com.marketplace.commerce.application.dto.AuthResponseDto;
import com.marketplace.commerce.application.dto.LoginRequestDto;
import com.marketplace.commerce.application.dto.RegisterRequestDto;
import com.marketplace.commerce.application.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Customer Auth", description = "Customer registration and login")
@RestController
@RequestMapping("/api/v1/commerce/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Register a new customer account")
    @PostMapping("/register")
    public ApiResponseDto<AuthResponseDto> register(
            @Valid @RequestBody RegisterRequestDto request,
            HttpServletRequest httpRequest
    ) {
        AuthResponseDto result = authService.register(request);
        return ApiResponseDto.success(result, "Registration successful.", httpRequest.getRequestURI());
    }

    @Operation(summary = "Login with email and password")
    @PostMapping("/login")
    public ApiResponseDto<AuthResponseDto> login(
            @Valid @RequestBody LoginRequestDto request,
            HttpServletRequest httpRequest
    ) {
        AuthResponseDto result = authService.login(request);
        return ApiResponseDto.success(result, "Login successful.", httpRequest.getRequestURI());
    }
}