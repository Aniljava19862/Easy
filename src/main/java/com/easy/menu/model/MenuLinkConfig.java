package com.easy.menu.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "menu_link_config") // Defines the actual table name in the database
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuLinkConfig {
    @Id
    @Column(name = "id", nullable = false, unique = true)
    private String id; // Primary Key, typically a UUID

    @Column(name = "table_definition_id_ref", nullable = false, unique = true)
    private String tableDefinitionIdRef; // Foreign key to the TableDefinition entity's ID

    @Column(name = "project_config_id_ref", nullable = false)
    private String projectConfigIdRef; // Foreign key to the ProjectConfig entity's ID

    @Column(name = "link_name", nullable = false)
    private String linkName; // The display name for the menu item (e.g., "Manage Employees")

    @Column(name = "link_path", nullable = false)
    private String linkPath; // The relative path for the frontend to navigate to (e.g., "/projects/{projectId}/data/{tableName}")

    @Column(name = "menu_order") // Optional: for ordering menu items in the UI
    private Integer menuOrder;

    @Column(name = "icon_class") // Optional: for a CSS class to display an icon
    private String iconClass;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}