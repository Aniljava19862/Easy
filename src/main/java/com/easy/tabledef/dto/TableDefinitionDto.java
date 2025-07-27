package com.easy.tabledef.dto;

import com.easy.tabledef.model.TableDefinition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TableDefinitionDto {
    private String id;
    private String tableName;
    private String appSuffix;
    private String finalTableName;
    private String description;
    private String projectConfigIdRef; // To indicate which project this table belongs to

    // ADD THIS FIELD to include column definitions in the DTO
    private List<ColumnDefinitionDto> columns;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Converts a TableDefinition entity to a TableDefinitionDto.
     *
     * @param entity The TableDefinition entity.
     * @return A new TableDefinitionDto.
     */
    public static TableDefinitionDto fromEntity(TableDefinition entity) {
        if (entity == null) {
            return null;
        }
        return TableDefinitionDto.builder()
                .id(entity.getId())
                .tableName(entity.getTableName())
                .appSuffix(entity.getAppSuffix())
                .finalTableName(entity.getFinalTableName())
                .description(entity.getDescription())
                .projectConfigIdRef(entity.getProjectConfigIdRef())
                // IMPORTANT: Map the list of ColumnDefinition entities to ColumnDefinitionDto
                .columns(entity.getColumns() != null ?
                        entity.getColumns().stream()
                                .map(ColumnDefinitionDto::fromEntity)
                                .collect(Collectors.toList()) :
                        null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}