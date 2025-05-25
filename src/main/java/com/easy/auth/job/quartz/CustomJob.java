package com.easy.auth.job.quartz;

import com.easy.auth.customlogic.service.CustomLogicService;
import com.easy.auth.job.model.JobDefinition;
import com.easy.auth.job.model.JobExecution;
import com.easy.auth.job.repository.JobExecutionRepository;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Component
public class CustomJob implements Job {

    private static final Logger log = LoggerFactory.getLogger(CustomJob.class);

    // Autowired by Quartz's SpringBeanJobFactory
    @Autowired
    private CustomLogicService customLogicService;
    @Autowired
    private JobExecutionRepository jobExecutionRepository;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDefinition jobDefinition = (JobDefinition) context.getJobDetail().getJobDataMap().get("jobDefinition");
        Map<String, Object> jobParams = (Map<String, Object>) context.getJobDetail().getJobDataMap().get("jobParams");

        if (jobDefinition == null) {
            log.error("JobDefinition not found in JobExecutionContext for job: {}", context.getJobDetail().getKey());
            return;
        }

        JobExecution jobExecution = new JobExecution();
        jobExecution.setJobDefinition(jobDefinition);
        jobExecution.setStartTime(LocalDateTime.now());
        jobExecution.setStatus("RUNNING");
        jobExecutionRepository.save(jobExecution); // Persist initial state

        log.info("Executing job: {} (Type: {})", jobDefinition.getName(), jobDefinition.getJobType());

        try {
            switch (jobDefinition.getJobType()) {
                case "CUSTOM_LOGIC":
                    if (jobDefinition.getAssociatedLogicId() == null) {
                        throw new IllegalStateException("Associated logic ID is missing for CUSTOM_LOGIC job.");
                    }
                    Object result = customLogicService.executeCustomLogic(jobDefinition.getAssociatedLogicId(), jobParams);
                    jobExecution.setLogs("Custom logic executed successfully. Result: " + result);
                    jobExecution.setStatus("SUCCESS");
                    break;
                case "WORKFLOW_TASK":
                    // TODO: Trigger Activiti workflow task here
                    jobExecution.setLogs("Workflow task triggered (NOT YET IMPLEMENTED)");
                    jobExecution.setStatus("SUCCESS");
                    break;
                default:
                    jobExecution.setLogs("Unsupported job type: " + jobDefinition.getJobType());
                    jobExecution.setStatus("FAILED");
                    log.error("Unsupported job type: {}", jobDefinition.getJobType());
                    break;
            }
        } catch (Exception e) {
            log.error("Job execution failed for job {}: {}", jobDefinition.getName(), e.getMessage(), e);
            jobExecution.setStatus("FAILED");
            jobExecution.setErrorMessage(e.getMessage());
            jobExecution.setLogs(jobExecution.getLogs() + "\nError: " + e.getMessage());
            throw new JobExecutionException(e); // Re-throw to inform Quartz
        } finally {
            jobExecution.setEndTime(LocalDateTime.now());
            jobExecutionRepository.save(jobExecution); // Persist final state
        }
    }
}
