package com.nadila.customer_management_api.controller;

import com.nadila.customer_management_api.dto.responseDto.ApiResponse;
import com.nadila.customer_management_api.enums.ResponseStatus;
import com.nadila.customer_management_api.service.addressService.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/addresses")
public class AddressController {

    private final AddressService addressService;

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(@PathVariable Long id) {

        addressService.deleteAddress(id);

        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .status(ResponseStatus.SUCCESS)
                        .message("Address deleted successfully")
                        .data(null)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }
}
