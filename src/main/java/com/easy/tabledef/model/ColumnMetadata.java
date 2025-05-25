package com.easy.tabledef.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "column_metadata")
@Data
public class ColumnMetadata {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_id", nullable = false)
    private TableMetadata tableMetadata;
    private String columnName;
    private String dataType; // e.g., "VARCHAR", "INTEGER", "BOOLEAN", "TIMESTAMP"
    private Integer length; // For VARCHAR
    private boolean isNullable;
    private boolean isPrimaryKey;
    private boolean isUnique;
    private boolean isForeignKey;
    private Long referencesTableId; // FK to TableMetadata
    private String referencesColumnName; // Column name in referenced table
}
