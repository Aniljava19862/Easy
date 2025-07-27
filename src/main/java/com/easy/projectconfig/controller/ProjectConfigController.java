package com.easy.projectconfig.controller;

import com.easy.projectconfig.dto.ProjectConfigDto;
import com.easy.projectconfig.dto.ProjectConfigResponseDto;
import com.easy.projectconfig.model.ProjectConfig;
import com.easy.projectconfig.service.ProjectConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException; // For proper error handling

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/project-configs")
public class ProjectConfigController {

    @Autowired
    private ProjectConfigService projectConfigService;

    /**
     * Saves a new project configuration.
     * If a configuration with the same project name already exists, it will be updated.
     * The 'id' in the JSON (e.g., "prod_db_01") is now interpreted as the 'connection_name'
     * of a DatabaseConnectionDetails, and its UUID will be stored.
     *
     * @param projectConfig The JSON body representing the ProjectConfig to save.
     * @return The saved ProjectConfig object with HTTP 200 OK or 201 Created.
     */
    /**
     * Saves a new project configuration.
     * The backend generates the UUID for the project configuration itself.
     * The 'databaseConnectionNameReference' in the JSON (e.g., "prod_db_01")
     * is used to look up the UUID of the DatabaseConnectionDetails.
     *
     * @param projectConfig The JSON body representing the ProjectConfig to save.
     * @return A ResponseEntity containing only the generated UUID and project name of the saved config.
     */
    @PostMapping
    public ResponseEntity<ProjectConfigResponseDto> saveProjectConfig(@RequestBody ProjectConfig projectConfig) {
        try {
            ProjectConfig savedConfig = projectConfigService.saveProjectConfig(projectConfig);

            // Create the DTO with only the required fields
            ProjectConfigResponseDto responseDto = new ProjectConfigResponseDto(
                    savedConfig.getId(),
                    savedConfig.getProjectName()
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(responseDto); // 201 Created
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save project configuration: " + e.getMessage());
        }
    }

    /**
     * Retrieves a project configuration by its project name.
     *
     * @param projectName The name of the project to retrieve.
     * @return The ProjectConfig object with HTTP 200 OK, or 404 Not Found.
     */
    @GetMapping("/{projectName}")
    public ResponseEntity<ProjectConfig> getProjectConfig(@PathVariable String projectName) {
        Optional<ProjectConfig> projectConfig = projectConfigService.getProjectConfigByProjectName(projectName);
        return projectConfig.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Deletes a project configuration by its project name.
     *
     * @param projectName The name of the project to delete.
     * @return HTTP 204 No Content if deleted, or 404 Not Found if not existing.
     */
    @DeleteMapping("/{projectName}")
    public ResponseEntity<Void> deleteProjectConfig(@PathVariable String projectName) {
        boolean deleted = projectConfigService.deleteProjectConfig(projectName);
        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    /**
     * Retrieves all project configurations.
     * GET /api/project-configs
     *
     * @return ResponseEntity with a list of ProjectConfigDto or an error message.
     */
    @GetMapping("/getprojects")
    public ResponseEntity<?> getAllProjectConfigs() {
        try {
            List<ProjectConfigDto> projectConfigs = projectConfigService.getAllProjectConfigs();
            if (projectConfigs.isEmpty()) {
                Map<String, Object> errorBody = new HashMap<>();
                errorBody.put("message", "No project configurations found.");
                errorBody.put("status", HttpStatus.NOT_FOUND.value());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody);
            }
            return ResponseEntity.ok(projectConfigs);
        } catch (RuntimeException e) {
            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("message", "Failed to retrieve project configurations: " + e.getMessage());
            errorBody.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody);
        }
    }
}