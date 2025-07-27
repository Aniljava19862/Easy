package com.easy.projectconfig.service;

import com.easy.projectconfig.dto.ProjectConfigDto;
import com.easy.projectconfig.model.ProjectConfig;
import com.easy.projectconfig.repository.ProjectConfigRepository;
import com.easy.application.dbtest.service.DatabaseConnectivityService;
import com.easy.application.dbtest.data.DatabaseConnectionDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProjectConfigService {

    @Autowired
    private ProjectConfigRepository projectConfigRepository;

    @Autowired
    private DatabaseConnectivityService databaseConnectivityService;

    /**
     * Saves a new project configuration or updates an existing one.
     * Generates a UUID for the ProjectConfig's own ID.
     * Translates the connectionName (from ProjectConfig's 'databaseConnectionNameReference' field)
     * into the actual UUID from DatabaseConnectionDetails before saving.
     *
     * @param projectConfig The ProjectConfig object to save.
     * @return The saved ProjectConfig object.
     * @throws IllegalArgumentException if the referenced database connection name is not found or project name already exists.
     */
    @Transactional
    public ProjectConfig saveProjectConfig(ProjectConfig projectConfig) {
        // Handle unique project name: ensure no other project exists with the same name
        Optional<ProjectConfig> existingProjectByName = projectConfigRepository.findByProjectName(projectConfig.getProjectName());
        if (existingProjectByName.isPresent() && !existingProjectByName.get().getId().equals(projectConfig.getId())) {
            throw new IllegalArgumentException("Project with name '" + projectConfig.getProjectName() + "' already exists.");
        }

        // Get the connectionName from the transient field
        String connectionName = projectConfig.getDatabaseConnectionNameReference();
        if (connectionName == null || connectionName.trim().isEmpty()) {
            throw new IllegalArgumentException("Database connection name reference cannot be empty.");
        }

        // Look up the DatabaseConnectionDetails by its connectionName
        Optional<DatabaseConnectionDetails> dbDetails = databaseConnectivityService.getSavedConnectionByName(connectionName);

        if (dbDetails.isEmpty()) {
            throw new IllegalArgumentException("Database connection with name '" + connectionName + "' not found. Cannot save project configuration.");
        }

        // Set the databaseConnectionIdRef in ProjectConfig to the actual UUID of the found connection
        projectConfig.setDatabaseConnectionIdRef(dbDetails.get().getUuid());

        // The @PrePersist in ProjectConfig will generate its own 'id' UUID if it's new
        return projectConfigRepository.save(projectConfig);
    }

    /**
     * Retrieves a project configuration by its ProjectConfig's UUID.
     *
     * @param id The UUID of the project configuration.
     * @return An Optional containing the ProjectConfig if found, or empty otherwise.
     */
    @Transactional(readOnly = true)
    public Optional<ProjectConfig> getProjectConfigById(String id) {
        return projectConfigRepository.findById(id);
    }

    /**
     * Retrieves a project configuration by its project name.
     *
     * @param projectName The name of the project.
     * @return An Optional containing the ProjectConfig if found, or empty otherwise.
     */
    @Transactional(readOnly = true)
    public Optional<ProjectConfig> getProjectConfigByProjectName(String projectName) {
        return projectConfigRepository.findByProjectName(projectName);
    }

    /**
     * Deletes a project configuration by its ProjectConfig's UUID.
     *
     * @param id The UUID of the project to delete.
     * @return true if deleted, false if not found.
     */
    @Transactional
    public boolean deleteProjectConfig(String id) {
        if (projectConfigRepository.existsById(id)) {
            projectConfigRepository.deleteById(id);
            return true;
        }
        return false;
    }


    /**
     * Retrieves all project configurations.
     *
     * @return A list of ProjectConfigDto objects.
     */
    @Transactional(readOnly = true)
    public List<ProjectConfigDto> getAllProjectConfigs() {
        return projectConfigRepository.findAll().stream()
                .map(ProjectConfigDto::fromEntity) // Convert each entity to DTO
                .collect(Collectors.toList());
    }

}