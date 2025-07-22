package com.easy.tabledef.model;

import com.fasterxml.jackson.annotation.JsonManagedReference; // Ensure this is imported
import com.fasterxml.jackson.annotation.JsonInclude; // Add if you need it for serialization control
import com.fasterxml.jackson.annotation.JsonSetter;   // Add if you need custom deserialization for finalTableName

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "table_metadata",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"table_name"}),
                @UniqueConstraint(columnNames = {"final_table_name"})
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TableDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "table_name", nullable = false, unique = true)
    private String tableName; // Logical name, e.g., "CustomerData"

    @Column(name = "app_suffix")
    private String appSuffix; // Suffix for physical table name, e.g., "_crm" or "_test"

    @Column(name = "final_table_name", nullable = false, unique = true)
    // Removed @JsonInclude/@JsonSetter if not explicitly needed for finalTableName input as discussed
    private String finalTableName; // Physical table name, e.g., "customer_data_crm"

    @Column(name = "description")
    private String description;

    @OneToMany(mappedBy = "tableDefinition", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference("tableDefinitionReference") // <--- KEY CHANGE: ADD A NAME HERE
    private List<ColumnDefinition> columns;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void setTimestampsAndConvertNames() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        updatedAt = LocalDateTime.now();

        if (this.finalTableName == null || this.finalTableName.trim().isEmpty()) {
            if (this.tableName != null && !this.tableName.trim().isEmpty()) {
                String baseName = convertToSnakeCase(this.tableName);
                this.finalTableName = baseName + (this.appSuffix != null && !this.appSuffix.trim().isEmpty() ? "_" + convertToSnakeCase(this.appSuffix) : "");
            } else {
                throw new IllegalArgumentException("Cannot generate finalTableName: tableName is missing.");
            }
        } else {
            this.finalTableName = convertToSnakeCase(this.finalTableName);
        }
    }

    private String convertToSnakeCase(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        return name.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase();
    }
}