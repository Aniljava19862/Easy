package com.easy.auth.job.repository;



import com.easy.auth.job.model.JobDefinition;
import com.easy.auth.job.model.JobExecution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobExecutionRepository extends JpaRepository<JobExecution, Long> {

    List<JobExecution> findByJobDefinition(JobDefinition jobDefinition);
}