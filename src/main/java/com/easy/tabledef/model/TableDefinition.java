package com.easy.tabledef.model;

import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "table_definitions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TableDefinition {

    @Id
    @Column(name = "id", unique = true, nullable = false, length = 36)
    private String id;

    // --- Reference to ProjectConfig UUID ---
    @Column(name = "project_config_id_ref", nullable = false, length = 36)
    private String projectConfigIdRef; // Stores the UUID of the associated ProjectConfig

    @Column(name = "table_name", unique = true, nullable = false)
    private String tableName; // Logical name of the table (e.g., "CustomerData")

    // --- NEW: appSuffix field ---
    // This could be used as part of the unique finalTableName generation
    @Column(name = "app_suffix", length = 50)
    private String appSuffix;

    @Column(name = "final_table_name", unique = true, nullable = false)
    private String finalTableName; // Actual physical table name (e.g., "customer_data_uuid123")

    @Column(name = "description")
    private String description;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "tableDefinition", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("columnOrder ASC") // Assuming you have a columnOrder field in ColumnDefinition for UI display
    private List<ColumnDefinition> columns;

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