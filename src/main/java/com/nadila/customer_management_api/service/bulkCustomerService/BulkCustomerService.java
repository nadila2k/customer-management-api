package com.nadila.customer_management_api.service.bulkCustomerService;

import com.nadila.customer_management_api.dto.responseDto.BulkJobResponseDto;
import org.springframework.web.multipart.MultipartFile;

public interface BulkCustomerService {
    BulkJobResponseDto initiateBulkUpload(MultipartFile file);
    BulkJobResponseDto getJobStatus(String jobId);
}
