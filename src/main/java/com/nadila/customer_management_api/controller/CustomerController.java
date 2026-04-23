package com.nadila.customer_management_api.controller;

import com.nadila.customer_management_api.dto.requestDto.CustomerRequestDto;
import com.nadila.customer_management_api.dto.responseDto.ApiResponse;
import com.nadila.customer_management_api.dto.responseDto.CustomerResponseDto;
import com.nadila.customer_management_api.enums.ResponseStatus;
import com.nadila.customer_management_api.service.customerService.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private final CustomerService customerService;


    @PostMapping
    public ResponseEntity<ApiResponse<CustomerResponseDto>> createCustomer(
            @Valid @RequestBody CustomerRequestDto dto) {


        CustomerResponseDto response = customerService.createCustomer(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<CustomerResponseDto>builder()
                        .status(ResponseStatus.SUCCESS)
                        .message("Customer created successfully")
                        .data(response)
                        .timestamp(LocalDateTime.now())
                        .build());
    }
}
