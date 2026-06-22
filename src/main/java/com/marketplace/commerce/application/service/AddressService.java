package com.marketplace.commerce.application.service;

import com.marketplace.commerce.application.dto.AddressRequestDto;
import com.marketplace.commerce.application.dto.AddressResponseDto;
import com.marketplace.commerce.domain.model.CustomerAddress;
import com.marketplace.commerce.domain.repository.CustomerAddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final CustomerAddressRepository addressRepository;

    @Transactional(readOnly = true)
    public List<AddressResponseDto> listAddresses(UUID customerId) {
        return addressRepository.findByCustomerId(customerId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public AddressResponseDto addAddress(UUID customerId, AddressRequestDto request) {
        boolean makeDefault = Boolean.TRUE.equals(request.getIsDefault());

        if (makeDefault) {
            clearExistingDefault(customerId);
        }

        // First address for a customer is automatically the default
        boolean isFirstAddress = addressRepository.findByCustomerId(customerId).isEmpty();

        CustomerAddress address = CustomerAddress.builder()
                .customerId(customerId)
                .label(request.getLabel())
                .fullAddress(request.getFullAddress())
                .isDefault(makeDefault || isFirstAddress)
                .build();

        addressRepository.save(address);
        return toDto(address);
    }

    @Transactional
    public void deleteAddress(UUID customerId, UUID addressId) {
        CustomerAddress address = addressRepository.findByIdAndCustomerId(addressId, customerId)
                .orElseThrow(() -> new NoSuchElementException("Address not found."));
        addressRepository.delete(address);
    }

    @Transactional
    public AddressResponseDto setDefault(UUID customerId, UUID addressId) {
        CustomerAddress address = addressRepository.findByIdAndCustomerId(addressId, customerId)
                .orElseThrow(() -> new NoSuchElementException("Address not found."));

        clearExistingDefault(customerId);
        address.setIsDefault(true);
        addressRepository.save(address);

        return toDto(address);
    }

    /**
     * Resolves the address to use for an order — either a specific saved
     * address ID, or falls back to the customer's default address.
     * Used by CheckoutService.
     */
    @Transactional(readOnly = true)
    public String resolveAddressText(UUID customerId, UUID addressId) {
        if (addressId != null) {
            return addressRepository.findByIdAndCustomerId(addressId, customerId)
                    .orElseThrow(() -> new NoSuchElementException("Address not found."))
                    .getFullAddress();
        }
        return addressRepository.findByCustomerIdAndIsDefaultTrue(customerId)
                .orElseThrow(() -> new IllegalStateException(
                        "No shipping address available. Please add an address first."))
                .getFullAddress();
    }

    private void clearExistingDefault(UUID customerId) {
        addressRepository.findByCustomerIdAndIsDefaultTrue(customerId)
                .ifPresent(existing -> {
                    existing.setIsDefault(false);
                    addressRepository.save(existing);
                });
    }

    private AddressResponseDto toDto(CustomerAddress address) {
        return AddressResponseDto.builder()
                .id(address.getId())
                .label(address.getLabel())
                .fullAddress(address.getFullAddress())
                .isDefault(address.getIsDefault())
                .build();
    }
}