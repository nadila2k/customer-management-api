package com.nadila.customer_management_api.controller;

import com.nadila.customer_management_api.dto.responseDto.ApiResponse;
import com.nadila.customer_management_api.dto.responseDto.BulkJobResponseDto;
import com.nadila.customer_management_api.enums.ResponseStatus;
import com.nadila.customer_management_api.service.bulkCustomerService.BulkCustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/customers/bulk")
public class BulkCustomerController {

    private final BulkCustomerService bulkCustomerService;

    /**
     * POST /api/v1/customers/bulk/upload
     *
     * Upload an .xlsx file for bulk upsert (create + update).
     * Returns 202 Accepted immediately with a jobId — processing happens in background.
     *
     * Excel format:
     *   A = name
     *   B = dateOfBirth         (yyyy-MM-dd)
     *   C = nicNumber           ← upsert key
     *   D = phones              comma-separated  e.g.  +94771234561,+94771234562
     *   E = addressLine1s       pipe-separated   e.g.  123 Main St|45 Lake Rd
     *   F = addressLine2s       pipe-separated   e.g.  Floor 2|null
     *   G = cityNames           pipe-separated   e.g.  Colombo|Kandy
     *   H = familyMemberNics    comma-separated  e.g.  NIC002LK,NIC003LK  (optional)
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<BulkJobResponseDto>> uploadCustomers(
            @RequestParam("file") MultipartFile file) {

        BulkJobResponseDto response = bulkCustomerService.initiateBulkUpload(file);

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.<BulkJobResponseDto>builder()
                        .status(ResponseStatus.SUCCESS)
                        .message("Bulk upsert started — use jobId to poll progress")
                        .data(response)
                        .timestamp(LocalDateTime.now())
                        .build());
    }

    /**
     * GET /api/v1/customers/bulk/status/{jobId}
     *
     * Poll the status and progress of a running or completed bulk job.
     */
    @GetMapping("/status/{jobId}")
    public ResponseEntity<ApiResponse<BulkJobResponseDto>> getJobStatus(
            @PathVariable String jobId) {

        BulkJobResponseDto response = bulkCustomerService.getJobStatus(jobId);

        return ResponseEntity.ok(
                ApiResponse.<BulkJobResponseDto>builder()
                        .status(ResponseStatus.SUCCESS)
                        .message("Job status fetched successfully")
                        .data(response)
                        .timestamp(LocalDateTime.now())
                        .build());
    }
}