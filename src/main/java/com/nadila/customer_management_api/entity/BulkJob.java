package com.nadila.customer_management_api.entity;

import com.nadila.customer_management_api.enums.BulkJobStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bulk_job")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class BulkJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", unique = true, nullable = false)
    private String jobId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BulkJobStatus status;

    @Column(name = "total_records")
    private int totalRecords;

    @Column(name = "processed_records")
    private int processedRecords;

    @Column(name = "inserted_count")
    private int insertedCount;

    @Column(name = "updated_count")
    private int updatedCount;

    @Column(name = "failed_records")
    private int failedRecords;

    @Column(name = "error_details", columnDefinition = "TEXT")
    private String errorDetails;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
