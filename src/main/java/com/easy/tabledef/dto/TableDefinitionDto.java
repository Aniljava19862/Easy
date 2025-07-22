package com.easy.tabledef.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TableDefinitionDto {
    private Long id;
    private String tableName;
    private String appSuffix;
    private String finalTableName;
    private String description;
    private List<ColumnDefinitionDto> columns; // This will hold DTOs of columns
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Static factory method to convert entity to DTO
    public static TableDefinitionDto fromEntity(com.easy.tabledef.model.TableDefinition entity) {
        if (entity == null) {
            return null;
        }

        List<ColumnDefinitionDto> columnDtos = null;
        // CRITICAL: Check if the lazy collection is initialized before accessing it.
        // If Hibernate.isInitialized() is false, it means the columns haven't been loaded yet.
        // In this case, we avoid accessing them to prevent LazyInitializationException.
        // The service layer should ensure they are initialized if needed.
        if (entity.getColumns() != null && org.hibernate.Hibernate.isInitialized(entity.getColumns())) {
            columnDtos = entity.getColumns().stream()
                    .map(ColumnDefinitionDto::fromEntity)
                    .collect(Collectors.toList());
        }

        return new TableDefinitionDto(
                entity.getId(),
                entity.getTableName(),
                entity.getAppSuffix(),
                entity.getFinalTableName(),
                entity.getDescription(),
                columnDtos, // Pass the DTO list (can be null if not initialized)
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}