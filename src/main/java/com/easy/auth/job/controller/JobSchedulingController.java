package com.easy.auth.job.controller;


import com.easy.auth.job.DynamicDataCleanupJob;
import com.easy.auth.job.service.SchedulerService;
import org.quartz.JobDataMap;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;

@RestController
@RequestMapping("/api/jobs")
public class JobSchedulingController {

    @Autowired
    private SchedulerService schedulerService;

    @PostMapping("/schedule/cleanup/onetime")
    public ResponseEntity<String> scheduleCleanupJobOneTime(@RequestParam String jobName,
                                                            @RequestParam(required = false) String jobGroup,
                                                            @RequestParam String runTimeIso, // YYYY-MM-DDTHH:MM:SS
                                                            @RequestBody(required = false) Map<String, String> data) {
        try {
            jobGroup = (jobGroup == null || jobGroup.isEmpty()) ? "default-cleanup" : jobGroup;
            LocalDateTime localDateTime = LocalDateTime.parse(runTimeIso);
            Date runTime = Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());

            JobDataMap jobDataMap = new JobDataMap();
            if (data != null) {
                jobDataMap.putAll(data);
            }
            jobDataMap.put("taskName", "Cleanup specific task: " + jobName); // Example additional data

            schedulerService.scheduleOneTimeJob(DynamicDataCleanupJob.class, jobName, jobGroup, runTime, jobDataMap);
            return ResponseEntity.ok("One-time cleanup job '" + jobName + "' scheduled successfully.");
        } catch (SchedulerException | java.time.format.DateTimeParseException e) {
            return ResponseEntity.status(500).body("Error scheduling job: " + e.getMessage());
        }
    }

    @PostMapping("/schedule/cleanup/cron")
    public ResponseEntity<String> scheduleCleanupJobCron(@RequestParam String jobName,
                                                         @RequestParam(required = false) String jobGroup,
                                                         @RequestParam String cronExpression, // e.g., "0 0 0 * * ?" for daily at midnight
                                                         @RequestBody(required = false) Map<String, String> data) {
        try {
            jobGroup = (jobGroup == null || jobGroup.isEmpty()) ? "default-cleanup" : jobGroup;
            JobDataMap jobDataMap = new JobDataMap();
            if (data != null) {
                jobDataMap.putAll(data);
            }
            jobDataMap.put("taskName", "Cron cleanup task: " + jobName); // Example additional data

            schedulerService.scheduleCronJob(DynamicDataCleanupJob.class, jobName, jobGroup, cronExpression, jobDataMap);
            return ResponseEntity.ok("Cron cleanup job '" + jobName + "' scheduled successfully.");
        } catch (SchedulerException e) {
            return ResponseEntity.status(500).body("Error scheduling cron job: " + e.getMessage());
        }
    }

    @DeleteMapping("/unschedule/{jobName}/{jobGroup}")
    public ResponseEntity<String> unscheduleJob(@PathVariable String jobName, @PathVariable String jobGroup) {
        try {
            if (schedulerService.doesJobExist(jobName, jobGroup)) {
                schedulerService.unscheduleJob(jobName, jobGroup);
                return ResponseEntity.ok("Job '" + jobName + "' in group '" + jobGroup + "' unscheduled and deleted.");
            } else {
                return ResponseEntity.badRequest().body("Job '" + jobName + "' in group '" + jobGroup + "' does not exist.");
            }
        } catch (SchedulerException e) {
            return ResponseEntity.status(500).body("Error unscheduling job: " + e.getMessage());
        }
    }
}
