package com.easy.tabledef.dto;

import com.easy.tabledef.model.ColumnDefinition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ColumnDefinitionDto {
    private String id;
    private String columnName;
    private String displayName;
    private String columnType;
    private boolean nullable;
    private boolean unique;
    private boolean primaryKey;
    private String defaultValue;
    private boolean autoIncrement; // If you have this property
    private String referencedTableIdRef; // ID of the referenced TableDefinition
    private String referencedColumnIdRef; // ID of the referenced ColumnDefinition (primary/unique key)
    private String referencedTableLogicalName; // Logical name of the referenced table
    private String referencedColumnLogicalName; // Logical name of the referenced column
    private boolean reference; // Indicates if this column is a reference to another table

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Converts a ColumnDefinition entity to a ColumnDefinitionDto.
     *
     * @param entity The ColumnDefinition entity.
     * @return A new ColumnDefinitionDto.
     */
    public static ColumnDefinitionDto fromEntity(ColumnDefinition entity) {
        if (entity == null) {
            return null;
        }
        return ColumnDefinitionDto.builder()
                .id(entity.getId())
                .columnName(entity.getColumnName())
                .displayName(entity.getDisplayName())
                .columnType(entity.getColumnType())
                .nullable(entity.isNullable())
                .unique(entity.isUnique())
                .primaryKey(entity.isPrimaryKey())
                .defaultValue(entity.getDefaultValue())
                // Make sure your ColumnDefinition entity has this getter
                .referencedTableIdRef(entity.getReferencedTableIdRef())
                .referencedColumnIdRef(entity.getReferencedColumnIdRef())
                .referencedTableLogicalName(entity.getReferencedTableLogicalName())
                .referencedColumnLogicalName(entity.getReferencedColumnLogicalName())
                .reference(entity.isReference())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}