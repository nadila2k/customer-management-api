package com.nadila.customer_management_api.dto.responseDto;

import com.nadila.customer_management_api.enums.BulkJobStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BulkJobResponseDto {
    private String jobId;
    private BulkJobStatus status;
    private int totalRecords;
    private int processedRecords;
    private int failedRecords;
    private String errorDetails;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
