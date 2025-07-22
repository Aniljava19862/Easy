package com.easy.tabledef.model;

import jakarta.persistence.*;
import lombok.Data; // <<< ADD THIS LINE for Lombok
import lombok.EqualsAndHashCode; // <<< ADD THIS LINE for Lombok
import lombok.ToString; // <<< ADD THIS LINE for Lombok

@Entity // <<< ADD THIS LINE for JPA mapping
@Table(name = "column_definition") // <<< ADD THIS LINE for JPA table name
@Data // <<< ADD THIS LINE: Lombok will now generate getters/setters for all fields, including tableDefinition
@EqualsAndHashCode(exclude = "tableDefinition") // Exclude the parent to prevent stack overflow
@ToString(exclude = "tableDefinition") // Exclude the parent to prevent stack overflow
public class ColumnDefinition {
    @Id // <<< ADD THIS LINE for primary key
    @GeneratedValue(strategy = GenerationType.IDENTITY) // <<< ADD THIS LINE for auto-incrementing ID
    private Long id; // <<< ADD THIS LINE for primary key

    private String columnName;
    private String displayName;
    private String columnType; // e.g., "Currency", "Text", "Number"
    private boolean isPrimaryKey;
    private boolean createIndex;

    // Getters and Setters (Lombok's @Data will generate these if you remove manual ones)
    // If you prefer to keep manual getters/setters, you must manually add:
    // public TableDefinition getTableDefinition() { return tableDefinition; }
    // public void setTableDefinition(TableDefinition tableDefinition) { this.tableDefinition = tableDefinition; }

    @ManyToOne(fetch = FetchType.LAZY) // Many ColumnDefinitions map to One TableDefinition
    @JoinColumn(name = "table_definition_id", nullable = false) // Foreign key column in 'column_definition' table
    private TableDefinition tableDefinition; // <<< THIS FIELD WAS MISSING AND IS CRUCIAL FOR THE RELATIONSHIP
}