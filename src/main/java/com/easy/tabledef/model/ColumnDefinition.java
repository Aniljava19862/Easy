package com.easy.tabledef.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "column_definitions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ColumnDefinition {

    @Id
    @Column(name = "id", unique = true, nullable = false, length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_definition_id", nullable = false)
    private TableDefinition tableDefinition;

    @Column(name = "column_name", nullable = false)
    private String columnName;

    @Column(name = "display_name") // Added based on your previous input
    private String displayName;

    @Column(name = "column_type", nullable = false)
    private String columnType; // e.g., "VARCHAR", "INT", "BOOLEAN", "DATE", "UUID"

    // --- NEW FIELDS FOR REFERENCE COLUMNS ---
    @Column(name = "is_reference")
    private boolean isReference; // True if this column refers to another dynamic table

    @Column(name = "referenced_table_id_ref", length = 36) // UUID of the TableDefinition it references
    private String referencedTableIdRef;

    @Column(name = "referenced_column_id_ref", length = 36) // UUID of the ColumnDefinition in the referenced table (e.g., system_row_id's column definition UUID)
    private String referencedColumnIdRef;

    @Column(name = "referenced_table_logical_name") // Store logical name for convenience
    private String referencedTableLogicalName;

    @Column(name = "referenced_column_logical_name") // Store logical name for convenience
    private String referencedColumnLogicalName;
    // --- END NEW FIELDS ---

    @Column(name = "is_primary_key")
    private boolean isPrimaryKey;

    @Column(name = "is_nullable")
    private boolean isNullable;

    @Column(name = "is_unique")
    private boolean isUnique;

    @Column(name = "is_create_index")
    private boolean isCreateIndex;
    @Column(name = "column_order")
    private Integer columnOrder;

    @Column(name = "default_value")
    private String defaultValue;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void generateIdAndTimestamps() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    public void setUpdatedAt() {
        this.updatedAt = LocalDateTime.now();
    }
}