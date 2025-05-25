package com.easy.auth.job.repository;


import com.easy.auth.job.model.JobDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobDefinitionRepository extends JpaRepository<JobDefinition, Long> {
    List<JobDefinition> findByEnabledTrue();
}
