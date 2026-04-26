package com.nadila.customer_management_api.controller;

import com.nadila.customer_management_api.dto.responseDto.ApiResponse;
import com.nadila.customer_management_api.enums.ResponseStatus;
import com.nadila.customer_management_api.service.phoneService.PhoneService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/phones")
public class PhoneController {

    private final PhoneService phoneService;

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePhone(@PathVariable Long id) {

        phoneService.deletePhone(id);

        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .status(ResponseStatus.SUCCESS)
                        .message("Phone deleted successfully")
                        .data(null)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }
}
