package com.easy.config;

import org.quartz.spi.JobFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
public class QuartzConfig {

    private final DataSource dataSource;
    private final ApplicationContext applicationContext;

    public QuartzConfig(DataSource dataSource, ApplicationContext applicationContext) {
        this.dataSource = dataSource;
        this.applicationContext = applicationContext;
    }

    @Bean
    public JobFactory springBeanJobFactory() {
        AutoWiringSpringBeanJobFactory jobFactory = new AutoWiringSpringBeanJobFactory();
        jobFactory.setApplicationContext(applicationContext);
        return jobFactory;
    }

    @Bean
    public SchedulerFactoryBean schedulerFactoryBean() {
        SchedulerFactoryBean schedulerFactoryBean = new SchedulerFactoryBean();
        schedulerFactoryBean.setDataSource(dataSource);
        schedulerFactoryBean.setJobFactory(springBeanJobFactory());
        schedulerFactoryBean.setOverwriteExistingJobs(true); // Allow overwriting jobs on startup
        schedulerFactoryBean.setApplicationContextSchedulerContextKey("applicationContext");

        // Customize Quartz properties if needed
        Properties quartzProperties = new Properties();
        quartzProperties.setProperty("org.quartz.scheduler.instanceName", "DynamicAppScheduler");
        quartzProperties.setProperty("org.quartz.scheduler.instanceId", "AUTO");
        quartzProperties.setProperty("org.quartz.threadPool.threadCount", "10");
        quartzProperties.setProperty("org.quartz.jobStore.driverDelegateClass", "org.quartz.impl.jdbcjobstore.PostgreSQLDelegate");
        schedulerFactoryBean.setQuartzProperties(quartzProperties);

        return schedulerFactoryBean;
    }
}
