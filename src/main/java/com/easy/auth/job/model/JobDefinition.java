package com.easy.auth.job.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "job_definitions")
@Data
public class JobDefinition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String description;
    private String jobType; // e.g., "CUSTOM_LOGIC", "WORKFLOW_TASK"
    private Long associatedLogicId; // FK to CustomLogic or other entity
    private String cronExpression; // For scheduled jobs
    private boolean enabled;
    private Long createdByUserId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
