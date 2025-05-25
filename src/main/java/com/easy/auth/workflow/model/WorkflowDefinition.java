package com.easy.auth.workflow.model;


import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "workflow_definitions")
@Data
public class WorkflowDefinition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String description;
    @Column(columnDefinition = "TEXT")
    private String bpmnXml; // Stores the BPMN XML content
    private String processDefinitionKey; // Activiti's key for the deployed process
    private Long createdByUserId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
