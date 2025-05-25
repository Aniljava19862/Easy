package com.easy.auth.job.controller;

import com.easy.auth.job.model.JobDefinition;
import com.easy.auth.job.model.JobExecution;
import com.easy.auth.job.service.JobService;
import org.quartz.SchedulerException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping
    public ResponseEntity<JobDefinition> createJob(@RequestBody JobDefinition jobDefinition) throws SchedulerException {
        JobDefinition createdJob = jobService.createJob(jobDefinition);
        return new ResponseEntity<>(createdJob, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<JobDefinition>> getAllJobs() {
        List<JobDefinition> jobs = jobService.getAllJobDefinitions();
        return ResponseEntity.ok(jobs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobDefinition> getJobById(@PathVariable Long id) {
        return jobService.getJobDefinitionById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<JobDefinition> updateJob(@PathVariable Long id, @RequestBody JobDefinition jobDefinition) throws SchedulerException {
        JobDefinition updatedJob = jobService.updateJob(id, jobDefinition);
        return ResponseEntity.ok(updatedJob);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJob(@PathVariable Long id) throws SchedulerException {
        jobService.deleteJob(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/trigger")
    public ResponseEntity<Void> triggerJob(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> params) throws SchedulerException {
        jobService.triggerJobNow(id, params != null ? params : Map.of());
        return ResponseEntity.accepted().build(); // Accepted means the job is queued
    }

    @GetMapping("/{id}/executions")
    public ResponseEntity<List<JobExecution>> getJobExecutions(@PathVariable Long id) {
        List<JobExecution> executions = jobService.getJobExecutionsForJob(id);
        return ResponseEntity.ok(executions);
    }
}
