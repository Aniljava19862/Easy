package com.easy.application.dbtest.data;

import jakarta.persistence.Column; // Make sure to import from jakarta.persistence
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor; // Add this for JPA
import lombok.AllArgsConstructor; // Add this for convenience

@Entity // Marks this class as a JPA entity
@Table(name = "database_connections") // Defines the table name
@Data
@NoArgsConstructor // Required by JPA
@AllArgsConstructor // Convenience constructor
public class DatabaseConnectionDetails {

    @Id // Marks this as the primary key
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-incrementing ID
    private Long id; // Unique identifier for each saved connection

    @NotBlank
    @Column(name = "db_type") // Explicit column name
    private String dbType; // e.g., "mysql", "oracle", "postgresql", "sqlserver"

    @NotBlank
    @Column(name = "database_ip")
    private String dataBaseIp;

    @Column(name = "database_port")
    private int dataBasePort; // Optional, might be inferred for common types

    @NotBlank
    @Column(name = "connection_name", unique = true) // Ensures connection names are unique
    private String connectionName; // Schema, SID, or a user-given name for this connection

    @NotBlank
    @Column(name = "db_user_name")
    private String dbUserName;

    // IMPORTANT: Storing passwords directly in plain text in the DB is INSECURE.
    // For production, always hash or encrypt passwords before saving.
    @NotBlank
    @Column(name = "db_password")
    private String dbPassword;
}