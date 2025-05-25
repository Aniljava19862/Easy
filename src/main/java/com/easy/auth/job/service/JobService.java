package com.easy.auth.job.service;

import com.easy.auth.job.model.JobDefinition;
import com.easy.auth.job.model.JobExecution;
import com.easy.auth.job.quartz.CustomJob;
import com.easy.auth.job.repository.JobDefinitionRepository;
import com.easy.auth.job.repository.JobExecutionRepository;
import org.quartz.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class JobService {

    private final JobDefinitionRepository jobDefinitionRepository;
    private final JobExecutionRepository jobExecutionRepository;
    private final Scheduler scheduler;

    public JobService(JobDefinitionRepository jobDefinitionRepository,
                      JobExecutionRepository jobExecutionRepository,
                      Scheduler scheduler) {
        this.jobDefinitionRepository = jobDefinitionRepository;
        this.jobExecutionRepository = jobExecutionRepository;
        this.scheduler = scheduler;
    }

    @Transactional
    public JobDefinition createJob(JobDefinition jobDefinition) throws SchedulerException {
        jobDefinition.setCreatedAt(LocalDateTime.now());
        jobDefinition.setUpdatedAt(LocalDateTime.now());
        // TODO: Set createdByUserId
        JobDefinition savedJob = jobDefinitionRepository.save(jobDefinition);

        if (savedJob.isEnabled() && savedJob.getCronExpression() != null && !savedJob.getCronExpression().isEmpty()) {
            scheduleJob(savedJob);
        }
        return savedJob;
    }

    @Transactional
    public JobDefinition updateJob(Long id, JobDefinition updatedJobDefinition) throws SchedulerException {
        return jobDefinitionRepository.findById(id)
                .map(existingJob -> {
                    existingJob.setName(updatedJobDefinition.getName());
                    existingJob.setDescription(updatedJobDefinition.getDescription());
                    existingJob.setJobType(updatedJobDefinition.getJobType());
                    existingJob.setAssociatedLogicId(updatedJobDefinition.getAssociatedLogicId());
                    existingJob.setCronExpression(updatedJobDefinition.getCronExpression());
                    boolean wasEnabled = existingJob.isEnabled();
                    existingJob.setEnabled(updatedJobDefinition.isEnabled());
                    existingJob.setUpdatedAt(LocalDateTime.now());

                    JobDefinition savedJob = jobDefinitionRepository.save(existingJob);

                    // Reschedule if cron changed or enabled status changed
                    if (wasEnabled != savedJob.isEnabled() || !existingJob.getCronExpression().equals(updatedJobDefinition.getCronExpression())) {
                        try {
                            unscheduleJob(savedJob.getId()); // Remove old schedule
                        } catch (SchedulerException e) {
                            throw new RuntimeException(e);
                        }
                        if (savedJob.isEnabled() && savedJob.getCronExpression() != null && !savedJob.getCronExpression().isEmpty()) {
                            try {
                                scheduleJob(savedJob); // Schedule new one
                            } catch (SchedulerException e) {
                                throw new RuntimeException("Failed to reschedule job: " + e.getMessage(), e);
                            }
                        }
                    }
                    return savedJob;
                })
                .orElseThrow(() -> new RuntimeException("Job Definition not found with ID: " + id));
    }

    @Transactional
    public void deleteJob(Long id) throws SchedulerException {
        jobDefinitionRepository.findById(id).ifPresent(jobDefinition -> {
            try {
                unscheduleJob(jobDefinition.getId());
                jobDefinitionRepository.delete(jobDefinition);
            } catch (SchedulerException e) {
                throw new RuntimeException("Failed to delete and unschedule job: " + e.getMessage(), e);
            }
        });
    }

    public Optional<JobDefinition> getJobDefinitionById(Long id) {
        return jobDefinitionRepository.findById(id);
    }

    public List<JobDefinition> getAllJobDefinitions() {
        return jobDefinitionRepository.findAll();
    }

    public List<JobExecution> getJobExecutionsForJob(Long jobDefinitionId) {
        JobDefinition jobDefinition = jobDefinitionRepository.findById(jobDefinitionId)
                .orElseThrow(() -> new RuntimeException("Job Definition not found with ID: " + jobDefinitionId));
        return jobExecutionRepository.findByJobDefinition(jobDefinition);
    }

    public void triggerJobNow(Long jobDefinitionId, Map<String, Object> params) throws SchedulerException {
        JobDefinition jobDefinition = jobDefinitionRepository.findById(jobDefinitionId)
                .orElseThrow(() -> new RuntimeException("Job Definition not found with ID: " + jobDefinitionId));

        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("jobDefinition", jobDefinition);
        jobDataMap.put("jobParams", params); // Pass parameters to the job

        JobDetail jobDetail = JobBuilder.newJob(CustomJob.class)
                .withIdentity("adhoc-job-" + jobDefinition.getId() + "-" + System.currentTimeMillis(), "adhoc-group")
                .usingJobData(jobDataMap)
                .storeDurably(false) // Not durable for ad-hoc
                .build();

        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("adhoc-trigger-" + jobDefinition.getId() + "-" + System.currentTimeMillis(), "adhoc-group")
                .startAt(new Date()) // Start immediately
                .build();

        scheduler.scheduleJob(jobDetail, trigger);
    }


    private void scheduleJob(JobDefinition jobDefinition) throws SchedulerException {
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("jobDefinition", jobDefinition);
        // Any common parameters can be put here for scheduled jobs

        JobDetail jobDetail = JobBuilder.newJob(CustomJob.class)
                .withIdentity(jobDefinition.getId().toString(), "dynamic-jobs")
                .withDescription(jobDefinition.getDescription())
                .usingJobData(jobDataMap)
                .storeDurably(true) // Durable so it can exist without an active trigger
                .build();

        CronTrigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(jobDefinition.getId().toString(), "dynamic-triggers")
                .withSchedule(CronScheduleBuilder.cronSchedule(jobDefinition.getCronExpression()))
                .forJob(jobDetail)
                .build();

        scheduler.scheduleJob(jobDetail, trigger);
    }

    private void unscheduleJob(Long jobDefinitionId) throws SchedulerException {
        TriggerKey triggerKey = new TriggerKey(jobDefinitionId.toString(), "dynamic-triggers");
        if (scheduler.checkExists(triggerKey)) {
            scheduler.unscheduleJob(triggerKey);
        }
        JobKey jobKey = new JobKey(jobDefinitionId.toString(), "dynamic-jobs");
        if (scheduler.checkExists(jobKey)) {
            scheduler.deleteJob(jobKey); // Delete the jobDetail if no other triggers point to it
        }
    }

    // Call this on application startup to reschedule all enabled jobs
    public void rescheduleAllEnabledJobsOnStartup() throws SchedulerException {
        List<JobDefinition> enabledJobs = jobDefinitionRepository.findByEnabledTrue();
        for (JobDefinition job : enabledJobs) {
            if (job.getCronExpression() != null && !job.getCronExpression().isEmpty()) {
                // First, remove any existing schedules for this job, then reschedule
                unscheduleJob(job.getId());
                scheduleJob(job);
            }
        }
    }
}
