/*package com.easy.auth.workflow.service;



import com.easy.auth.workflow.model.WorkflowDefinition;
import com.easy.auth.workflow.repository.WorkflowDefinitionRepository;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.runtime.ProcessInstance;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class WorkflowService {

    private final WorkflowDefinitionRepository workflowDefinitionRepository;
    private final RepositoryService repositoryService; // Activiti for deploying BPMN
    private final RuntimeService runtimeService; // Activiti for starting process instances

    public WorkflowService(WorkflowDefinitionRepository workflowDefinitionRepository,
                           RepositoryService repositoryService,
                           RuntimeService runtimeService) {
        this.workflowDefinitionRepository = workflowDefinitionRepository;
        this.repositoryService = repositoryService;
        this.runtimeService = runtimeService;
    }

    @Transactional
    public WorkflowDefinition createWorkflow(WorkflowDefinition workflowDefinition) {
        workflowDefinition.setCreatedAt(LocalDateTime.now());
        workflowDefinition.setUpdatedAt(LocalDateTime.now());
        // TODO: Set createdByUserId

        // Deploy BPMN to Activiti
        Deployment deployment = repositoryService.createDeployment()
                .addString(workflowDefinition.getName() + ".bpmn20.xml", workflowDefinition.getBpmnXml())
                .deploy();

        // Assuming a single process definition per BPMN file for simplicity
        String processDefKey = repositoryService.createProcessDefinitionQuery()
                .deploymentId(deployment.getId())
                .singleResult()
                .getKey();
        workflowDefinition.setProcessDefinitionKey(processDefKey);

        return workflowDefinitionRepository.save(workflowDefinition);
    }

    @Transactional
    public WorkflowDefinition updateWorkflow(Long id, WorkflowDefinition updatedWorkflowDefinition) {
        return workflowDefinitionRepository.findById(id)
                .map(existingWorkflow -> {
                    existingWorkflow.setName(updatedWorkflowDefinition.getName());
                    existingWorkflow.setDescription(updatedWorkflowDefinition.getDescription());
                    existingWorkflow.setBpmnXml(updatedWorkflowDefinition.getBpmnXml());
                    existingWorkflow.setUpdatedAt(LocalDateTime.now());

                    // Re-deploying a workflow implies a new version in Activiti
                    // Old process instances will continue on the old version. New ones use the new.
                    Deployment deployment = repositoryService.createDeployment()
                            .addString(existingWorkflow.getName() + ".bpmn20.xml", updatedWorkflowDefinition.getBpmnXml())
                            .deploy();

                    String newProcessDefKey = repositoryService.createProcessDefinitionQuery()
                            .deploymentId(deployment.getId())
                            .singleResult()
                            .getKey();
                    existingWorkflow.setProcessDefinitionKey(newProcessDefKey); // Update to new key

                    return workflowDefinitionRepository.save(existingWorkflow);
                })
                .orElseThrow(() -> new RuntimeException("Workflow Definition not found with ID: " + id));
    }

    @Transactional
    public void deleteWorkflow(Long id) {
        workflowDefinitionRepository.findById(id).ifPresent(workflowDefinition -> {
            // Delete Activiti deployment (cascade will delete process definitions, etc.)
            // NOTE: Deleting a deployment will not stop running process instances by default.
            // You might need to handle active instances before deleting.
            repositoryService.deleteDeployment(workflowDefinition.getProcessDefinitionKey()); // Or by ID
            workflowDefinitionRepository.delete(workflowDefinition);
        });
    }

    public List<WorkflowDefinition> getAllWorkflowDefinitions() {
        return workflowDefinitionRepository.findAll();
    }

    public Optional<WorkflowDefinition> getWorkflowDefinitionById(Long id) {
        return workflowDefinitionRepository.findById(id);
    }

    public ProcessInstance startWorkflowInstance(Long workflowDefinitionId, Map<String, Object> variables) {
        WorkflowDefinition workflowDefinition = workflowDefinitionRepository.findById(workflowDefinitionId)
                .orElseThrow(() -> new RuntimeException("Workflow Definition not found with ID: " + workflowDefinitionId));

        return runtimeService.startProcessInstanceByKey(workflowDefinition.getProcessDefinitionKey(), variables);
    }

    // TODO: Add methods for querying process instances, completing tasks, etc.
}
*/