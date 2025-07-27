package com.easy.projectconfig.dto;

import com.easy.projectconfig.model.ProjectConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectConfigDto {
    private String id;
    private String projectName;
    private String description; // Assuming ProjectConfig has a description field
    private String databaseConnectionIdRef;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Static method to convert entity to DTO
    public static ProjectConfigDto fromEntity(ProjectConfig entity) {
        if (entity == null) {
            return null;
        }
        return ProjectConfigDto.builder()
                .id(entity.getId())
                .projectName(entity.getProjectName())

                .databaseConnectionIdRef(entity.getDatabaseConnectionIdRef())

                .build();
    }
}