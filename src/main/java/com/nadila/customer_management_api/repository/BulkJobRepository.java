package com.nadila.customer_management_api.repository;

import com.nadila.customer_management_api.entity.BulkJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BulkJobRepository extends JpaRepository<BulkJob, Long> {
    Optional<BulkJob> findByJobId(String jobId);
}
