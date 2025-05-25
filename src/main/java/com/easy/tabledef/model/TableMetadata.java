package com.easy.tabledef.model;


import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "table_metadata")
@Data
public class TableMetadata {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true, nullable = false)
    private String tableName;
    private String description;
    private Long createdByUserId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "tableMetadata", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ColumnMetadata> columns;
}
