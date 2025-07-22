package com.easy.tabledef.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "table_metadata")
@Data
@EqualsAndHashCode(exclude = "columns")
@ToString(exclude = "columns")
public class TableDefinition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String tableName; // Logical name for the table definition
    private String description;
    private Long createdByUserId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Column(unique = true, nullable = false) // Added finalTableName
    private String finalTableName; // The actual name of the physical database table

    private String appSuffix; // Added appSuffix

    @OneToMany(mappedBy = "tableDefinition", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ColumnDefinition> columns = new ArrayList<>();
}