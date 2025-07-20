package com.easy.auth.job.service;


import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class SchedulerService {

    private static final Logger logger = LoggerFactory.getLogger(SchedulerService.class);

    @Autowired
    private Scheduler scheduler; // Spring Boot auto-configures and provides this

    /**
     * Schedules a job to run at a specific time (one-time).
     * @param jobClass The class of the job to schedule.
     * @param jobName A unique name for the job.
     * @param jobGroup A group name for the job (e.g., "cleanup-jobs").
     * @param runTime The exact time the job should run.
     * @param data Additional data to pass to the job.
     * @throws SchedulerException if scheduling fails.
     */
    public void scheduleOneTimeJob(Class<? extends Job> jobClass, String jobName, String jobGroup, Date runTime, JobDataMap data) throws SchedulerException {
        JobDetail jobDetail = JobBuilder.newJob(jobClass)
                .withIdentity(jobName, jobGroup)
                .usingJobData(data)
                .build();

        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(jobName + "-trigger", jobGroup + "-triggers")
                .startAt(runTime)
                .withSchedule(SimpleScheduleBuilder.simpleSchedule().withMisfireHandlingInstructionFireNow()) // Fire immediately if missed
                .build();

        scheduler.scheduleJob(jobDetail, trigger);
        logger.info("Job '{}' in group '{}' scheduled to run once at {}", jobName, jobGroup, runTime);
    }

    /**
     * Schedules a job to run on a recurring Cron schedule.
     * @param jobClass The class of the job to schedule.
     * @param jobName A unique name for the job.
     * @param jobGroup A group name for the job.
     * @param cronExpression The Cron expression (e.g., "0 0 12 * * ?") for daily at noon.
     * @param data Additional data to pass to the job.
     * @throws SchedulerException if scheduling fails.
     */
    public void scheduleCronJob(Class<? extends Job> jobClass, String jobName, String jobGroup, String cronExpression, JobDataMap data) throws SchedulerException {
        JobDetail jobDetail = JobBuilder.newJob(jobClass)
                .withIdentity(jobName, jobGroup)
                .usingJobData(data)
                .build();

        CronTrigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(jobName + "-trigger", jobGroup + "-triggers")
                .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression))
                .build();

        scheduler.scheduleJob(jobDetail, trigger);
        logger.info("Job '{}' in group '{}' scheduled with cron expression: {}", jobName, jobGroup, cronExpression);
    }

    /**
     * Unschedules a job.
     * @param jobName The name of the job to unschedule.
     * @param jobGroup The group of the job to unschedule.
     * @throws SchedulerException if unscheduling fails.
     */
    public void unscheduleJob(String jobName, String jobGroup) throws SchedulerException {
        scheduler.unscheduleJob(new TriggerKey(jobName + "-trigger", jobGroup + "-triggers"));
        logger.info("Job '{}' in group '{}' unscheduled.", jobName, jobGroup);
        // Also delete the job if it's no longer needed
        scheduler.deleteJob(new JobKey(jobName, jobGroup));
        logger.info("JobDetail '{}' in group '{}' deleted.", jobName, jobGroup);
    }

    /**
     * Checks if a job exists.
     * @param jobName The name of the job.
     * @param jobGroup The group of the job.
     * @return true if the job exists, false otherwise.
     * @throws SchedulerException
     */
    public boolean doesJobExist(String jobName, String jobGroup) throws SchedulerException {
        return scheduler.checkExists(new JobKey(jobName, jobGroup));
    }
}