package com.easy.auth.job;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

// You can autowire Spring components into a Quartz Job if configured correctly
// (using SpringBeanJobFactory, which Spring Boot does by default with the starter)
@Component
public class DynamicDataCleanupJob implements Job {

    private static final Logger logger = LoggerFactory.getLogger(DynamicDataCleanupJob.class);

    // Example: If you needed to inject a service to clean data
    // @Autowired
    // private DynamicTableDataService dynamicTableDataService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        logger.info("Executing Dynamic Data Cleanup Job at {}", context.getFireTime());

        // This is where your actual cleanup logic would go.
        // For instance, delete old temporary records from dynamically created tables,
        // archive old data, or re-index something.

        // Example: Simulate cleanup work
        try {
            Thread.sleep(2000); // Simulate work
            logger.info("Dynamic Data Cleanup Job finished successfully.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Dynamic Data Cleanup Job interrupted.", e);
            throw new JobExecutionException("Job interrupted", e, false);
        }

        // You can retrieve job data map details if passed during scheduling
        String taskName = context.getJobDetail().getJobDataMap().getString("taskName");
        if (taskName != null) {
            logger.info("Job data map 'taskName': {}", taskName);
        }
    }
}
