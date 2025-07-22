package com.easy.tabledef.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ColumnDefinitionDto {
    private Long id;
    private String columnName;
    private String displayName;
    private String columnType;
    private boolean isPrimaryKey;
    private boolean createIndex;

    // Static factory method to convert entity to DTO
    public static ColumnDefinitionDto fromEntity(com.easy.tabledef.model.ColumnDefinition entity) {
        if (entity == null) {
            return null;
        }
        return new ColumnDefinitionDto(
                entity.getId(),
                entity.getColumnName(),
                entity.getDisplayName(),
                entity.getColumnType(),
                entity.isPrimaryKey(),
                entity.isCreateIndex()
        );
    }
}