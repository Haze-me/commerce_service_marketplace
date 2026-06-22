package com.marketplace.commerce.api.controller;

import com.marketplace.commerce.application.dto.ApiResponseDto;
import com.marketplace.commerce.application.dto.ChangePasswordRequestDto;
import com.marketplace.commerce.application.dto.CustomerProfileDto;
import com.marketplace.commerce.application.dto.UpdateProfileRequestDto;
import com.marketplace.commerce.application.service.CustomerProfileService;
import com.marketplace.commerce.infrastructure.security.CurrentCustomer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Customer Profile", description = "Authenticated customer's own profile management")
@RestController
@RequestMapping("/api/v1/commerce/profile")
@RequiredArgsConstructor
public class CustomerProfileController {

    private final CustomerProfileService profileService;

    @Operation(summary = "Get the authenticated customer's own profile")
    @GetMapping
    public ApiResponseDto<CustomerProfileDto> getProfile(HttpServletRequest request) {
        CustomerProfileDto profile = profileService.getProfile(CurrentCustomer.id());
        return ApiResponseDto.success(profile, "Success", request.getRequestURI());
    }

    @Operation(summary = "Update the authenticated customer's own profile")
    @PutMapping
    public ApiResponseDto<CustomerProfileDto> updateProfile(
            @Valid @RequestBody UpdateProfileRequestDto body,
            HttpServletRequest request
    ) {
        CustomerProfileDto profile = profileService.updateProfile(CurrentCustomer.id(), body);
        return ApiResponseDto.success(profile, "Profile updated successfully.", request.getRequestURI());
    }

    @Operation(summary = "Change the authenticated customer's own password")
    @PostMapping("/change-password")
    public ApiResponseDto<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequestDto body,
            HttpServletRequest request
    ) {
        profileService.changePassword(CurrentCustomer.id(), body);
        return ApiResponseDto.success(null, "Password changed successfully.", request.getRequestURI());
    }
}