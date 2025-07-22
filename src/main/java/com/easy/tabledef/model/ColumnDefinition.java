package com.easy.tabledef.model;

import com.fasterxml.jackson.annotation.JsonBackReference; // Ensure this is imported
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "column_metadata",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"table_definition_id", "column_name"})
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ColumnDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_definition_id", nullable = false)
    @JsonBackReference("tableDefinitionReference") // <--- KEY CHANGE: NAME MUST MATCH JsonManagedReference
    private TableDefinition tableDefinition; // This field should NOT be in your JSON request for ColumnDefinition

    @Column(name = "column_name", nullable = false)
    private String columnName;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "column_type", nullable = false)
    private String columnType; // e.g., "TEXT", "NUMBER", "DATE", "BOOLEAN", "CURRENCY"

    @Column(name = "is_primary_key", nullable = false)
    private boolean isPrimaryKey;

    @Column(name = "create_index", nullable = false)
    private boolean createIndex;
}