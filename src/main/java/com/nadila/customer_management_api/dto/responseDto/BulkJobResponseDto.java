package com.nadila.customer_management_api.dto.responseDto;

import com.nadila.customer_management_api.enums.BulkJobStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BulkJobResponseDto {

    private String        jobId;
    private BulkJobStatus status;

    private int totalRecords;
    private int insertedCount;   // new — rows created
    private int updatedCount;    // new — rows updated
    private int failedRecords;

    private ErrorSummary errors; // replaces plain errorDetails String

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ErrorSummary {
        private int                  totalErrors;
        private String               note;
        private List<BulkErrorEntryDto> items;
    }
}