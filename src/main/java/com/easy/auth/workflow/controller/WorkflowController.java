package com.easy.auth.workflow.controller;

import com.easy.auth.workflow.model.WorkflowDefinition;
import com.easy.auth.workflow.service.WorkflowService;
import org.activiti.engine.runtime.ProcessInstance;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/workflows")
public class WorkflowController {

    private final WorkflowService workflowService;

    public WorkflowController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @PostMapping
    public ResponseEntity<WorkflowDefinition> createWorkflow(@RequestBody WorkflowDefinition workflowDefinition) {
        WorkflowDefinition createdWorkflow = workflowService.createWorkflow(workflowDefinition);
        return new ResponseEntity<>(createdWorkflow, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<WorkflowDefinition>> getAllWorkflows() {
        List<WorkflowDefinition> workflows = workflowService.getAllWorkflowDefinitions();
        return ResponseEntity.ok(workflows);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkflowDefinition> getWorkflowById(@PathVariable Long id) {
        return workflowService.getWorkflowDefinitionById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<WorkflowDefinition> updateWorkflow(@PathVariable Long id, @RequestBody WorkflowDefinition workflowDefinition) {
        WorkflowDefinition updatedWorkflow = workflowService.updateWorkflow(id, workflowDefinition);
        return ResponseEntity.ok(updatedWorkflow);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWorkflow(@PathVariable Long id) {
        workflowService.deleteWorkflow(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<Map<String, String>> startWorkflowInstance(@PathVariable Long id,
                                                                     @RequestBody(required = false) Map<String, Object> variables) {
        ProcessInstance processInstance = workflowService.startWorkflowInstance(id, variables != null ? variables : Map.of());
        return ResponseEntity.ok(Map.of("processInstanceId", processInstance.getId()));
    }

    // TODO: Add endpoints for:
    // - Getting active tasks for a user
    // - Completing tasks
    // - Getting process instance history
}