package com.nadila.customer_management_api.controller;

import com.nadila.customer_management_api.dto.requestDto.CustomerRequestDto;
import com.nadila.customer_management_api.dto.responseDto.ApiResponse;
import com.nadila.customer_management_api.dto.responseDto.CustomerDetailResponseDto;
import com.nadila.customer_management_api.dto.responseDto.CustomerResponseDto;
import com.nadila.customer_management_api.enums.ResponseStatus;
import com.nadila.customer_management_api.service.customerService.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;

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

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<CustomerResponseDto>>> getAllCustomers() {

        List<CustomerResponseDto> customers = customerService.findAllCustomers();

        return ResponseEntity.ok(
                ApiResponse.<List<CustomerResponseDto>>builder()
                        .status(ResponseStatus.SUCCESS)
                        .message("Customers fetched successfully")
                        .data(customers)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCustomer(@PathVariable Long id) {

        customerService.deleteCustomer(id);

        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .status(ResponseStatus.SUCCESS)
                        .message("Customer deleted successfully")
                        .data(null)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }


    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerResponseDto>> updateCustomer(
            @PathVariable Long id,
            @Valid @RequestBody CustomerRequestDto dto) {

        CustomerResponseDto response = customerService.updateCustomer(id, dto);

        return ResponseEntity.ok(
                ApiResponse.<CustomerResponseDto>builder()
                        .status(ResponseStatus.SUCCESS)
                        .message("Customer updated successfully")
                        .data(response)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerDetailResponseDto>> getCustomerById(
            @PathVariable Long id) {

        CustomerDetailResponseDto customer = customerService.findCustomerById(id);

        return ResponseEntity.ok(
                ApiResponse.<CustomerDetailResponseDto>builder()
                        .status(ResponseStatus.SUCCESS)
                        .message("Customer fetched successfully")
                        .data(customer)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<CustomerResponseDto>>> searchCustomers(
            @RequestParam String keyword) {

        List<CustomerResponseDto> result = customerService.searchCustomerByNameOrNic(keyword);

        return ResponseEntity.ok(
                ApiResponse.<List<CustomerResponseDto>>builder()
                        .status(ResponseStatus.SUCCESS)
                        .message("Search results fetched successfully")
                        .data(result)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }


    @GetMapping("/paginated")
    public ResponseEntity<ApiResponse<Page<CustomerResponseDto>>> getCustomers(
            @RequestParam(defaultValue = "0")        int page,
            @RequestParam(defaultValue = "10")       int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc")     String sortDirection) {

        Page<CustomerResponseDto> customers = customerService.getCustomersPaginated(
                page, size, sortBy, sortDirection);

        return ResponseEntity.ok(
                ApiResponse.<Page<CustomerResponseDto>>builder()
                        .status(ResponseStatus.SUCCESS)
                        .message("Customers fetched successfully")
                        .data(customers)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }


}
