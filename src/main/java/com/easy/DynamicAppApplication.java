package com.easy;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling; // For Spring's @Scheduled if you opt for it

@SpringBootApplication
@EnableScheduling // Enable if you plan to use Spring's @Scheduled for simple jobs
public class DynamicAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(DynamicAppApplication.class, args);
    }

}