package com.nadila.customer_management_api.controller;

import com.nadila.customer_management_api.dto.responseDto.ApiResponse;
import com.nadila.customer_management_api.enums.ResponseStatus;
import com.nadila.customer_management_api.service.familyService.FamilyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/customers")
public class FamilyController {

    private final FamilyService familyService;


    @DeleteMapping("/{customerId}/family/{familyMemberId}")
    public ResponseEntity<ApiResponse<Void>> deleteFamilyMember(
            @PathVariable Long customerId,
            @PathVariable Long familyMemberId) {

        familyService.deleteFamilyMember(customerId, familyMemberId);

        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .status(ResponseStatus.SUCCESS)
                        .message("Family member removed successfully")
                        .data(null)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }
}