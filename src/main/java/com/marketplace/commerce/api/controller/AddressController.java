package com.marketplace.commerce.api.controller;

import com.marketplace.commerce.application.dto.AddressRequestDto;
import com.marketplace.commerce.application.dto.AddressResponseDto;
import com.marketplace.commerce.application.dto.ApiResponseDto;
import com.marketplace.commerce.application.service.AddressService;
import com.marketplace.commerce.infrastructure.security.CurrentCustomer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Customer Addresses", description = "Manage saved shipping addresses")
@RestController
@RequestMapping("/api/v1/commerce/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @Operation(summary = "List all saved addresses")
    @GetMapping
    public ApiResponseDto<List<AddressResponseDto>> list(HttpServletRequest request) {
        List<AddressResponseDto> addresses = addressService.listAddresses(CurrentCustomer.id());
        return ApiResponseDto.success(addresses, "Success", request.getRequestURI());
    }

    @Operation(summary = "Add a new saved address")
    @PostMapping
    public ApiResponseDto<AddressResponseDto> add(
            @Valid @RequestBody AddressRequestDto body,
            HttpServletRequest request
    ) {
        AddressResponseDto address = addressService.addAddress(CurrentCustomer.id(), body);
        return ApiResponseDto.success(address, "Address added.", request.getRequestURI());
    }

    @Operation(summary = "Delete a saved address")
    @DeleteMapping("/{addressId}")
    public ApiResponseDto<Void> delete(
            @PathVariable UUID addressId,
            HttpServletRequest request
    ) {
        addressService.deleteAddress(CurrentCustomer.id(), addressId);
        return ApiResponseDto.success(null, "Address deleted.", request.getRequestURI());
    }

    @Operation(summary = "Set an address as the default")
    @PutMapping("/{addressId}/default")
    public ApiResponseDto<AddressResponseDto> setDefault(
            @PathVariable UUID addressId,
            HttpServletRequest request
    ) {
        AddressResponseDto address = addressService.setDefault(CurrentCustomer.id(), addressId);
        return ApiResponseDto.success(address, "Default address updated.", request.getRequestURI());
    }
}