package com.easy.projectconfig.repository;

import com.easy.projectconfig.model.ProjectConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProjectConfigRepository extends JpaRepository<ProjectConfig, String> { // Changed Long to String (for the new UUID 'id')

    // Added a method to find by projectName, as it's no longer the @Id
    Optional<ProjectConfig> findByProjectName(String projectName);
}