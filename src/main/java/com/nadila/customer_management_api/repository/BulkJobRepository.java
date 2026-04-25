package com.nadila.customer_management_api.repository;

import com.nadila.customer_management_api.entity.BulkJob;

public interface BulkJobRepository extends JpaRepository<BulkJob, Long> {
    Optional<BulkJob> findByJobId(String jobId);
}
