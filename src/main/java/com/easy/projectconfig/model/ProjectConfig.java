package com.easy.projectconfig.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.vladmihalcea.hibernate.type.json.JsonType;
import org.hibernate.annotations.Type;

import java.util.List;
import java.util.UUID; // Import UUID

@Entity
@Table(name = "project_configurations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectConfig {

    @Id // The new primary key, generated as a UUID
    @Column(name = "id", unique = true, nullable = false, length = 36) // UUIDs are 36 chars
    private String id;

    // --- New Method to Generate UUID for the entity's primary key ---
    @PrePersist // This method runs before a new entity is persisted (saved for the first time)
    public void generateId() {
        if (this.id == null) { // Only generate if ID is not already set
            this.id = UUID.randomUUID().toString();
        }
    }

    // projectName is now a regular column, but can still be unique
    @Column(name = "project_name", unique = true, nullable = false)
    private String projectName;

    @Column(name = "layout", nullable = false)
    private String layout;

    // This column will store the UUID from DatabaseConnectionDetails
    @Column(name = "database_connection_id_ref", nullable = false, length = 36) // Renamed for clarity and storing UUID
    private String databaseConnectionIdRef;

    @Type(JsonType.class)
    @Column(name = "menu_config", columnDefinition = "json")
    private List<MenuItem> menu;

    // IMPORTANT: This field is transient. It's used *only* for receiving the input
    // and is not mapped to a database column. It helps in the service layer to look
    // up the actual UUID.
    @Transient
    private String databaseConnectionNameReference; // This holds the "prod_db_01" value from incoming JSON
}