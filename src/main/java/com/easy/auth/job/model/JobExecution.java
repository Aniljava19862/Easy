package com.easy.auth.job.model;


import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "job_executions")
@Data
public class JobExecution {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_definition_id", nullable = false)
    private JobDefinition jobDefinition;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status; // e.g., "PENDING", "RUNNING", "SUCCESS", "FAILED"
    @Column(columnDefinition = "TEXT")
    private String logs;
    private String errorMessage;
}
