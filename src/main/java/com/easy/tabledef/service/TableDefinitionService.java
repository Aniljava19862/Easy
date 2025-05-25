package com.easy.tabledef.service;


import com.easy.tabledef.model.TableMetadata;
import com.easy.tabledef.repository.TableDefinitionRepository;
import com.easy.util.DynamicTableDdlGenerator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TableDefinitionService {

    private final TableDefinitionRepository tableDefinitionRepository;
    private final JdbcTemplate jdbcTemplate;
    private final DynamicTableDdlGenerator ddlGenerator;

    public TableDefinitionService(TableDefinitionRepository tableDefinitionRepository,
                                  JdbcTemplate jdbcTemplate,
                                  DynamicTableDdlGenerator ddlGenerator) {
        this.tableDefinitionRepository = tableDefinitionRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.ddlGenerator = ddlGenerator;
    }

    @Transactional
    public TableMetadata createTable(TableMetadata tableMetadata) {
        // Basic validation: table name should be safe for SQL
        if (!tableMetadata.getTableName().matches("^[a-zA-Z0-9_]+$")) {
            throw new IllegalArgumentException("Invalid table name.");
        }

        tableMetadata.setCreatedAt(LocalDateTime.now());
        tableMetadata.setUpdatedAt(LocalDateTime.now());
        tableMetadata.getColumns().forEach(col -> col.setTableMetadata(tableMetadata));

        // Generate DDL for CREATE TABLE
        String createTableSql = ddlGenerator.generateCreateTableSql(tableMetadata);
        jdbcTemplate.execute(createTableSql);

        // Save metadata to application's schema
        return tableDefinitionRepository.save(tableMetadata);
    }

    @Transactional
    public TableMetadata updateTable(Long id, TableMetadata updatedTableMetadata) {
        return tableDefinitionRepository.findById(id)
                .map(existingTable -> {
                    // TODO: Implement complex ALTER TABLE logic here.
                    // This is VERY tricky: adding/removing columns, changing types, etc.
                    // Requires comparing old vs. new schema and generating appropriate DDL.
                    // Consider data migration implications. This is often the hardest part.
                    existingTable.setTableName(updatedTableMetadata.getTableName()); // Be careful allowing table name changes
                    existingTable.setDescription(updatedTableMetadata.getDescription());
                    existingTable.setUpdatedAt(LocalDateTime.now());

                    // For simplicity, this skeleton only updates metadata.
                    // A real implementation needs to generate ALTER TABLE DDL and execute it.

                    return tableDefinitionRepository.save(existingTable);
                })
                .orElseThrow(() -> new RuntimeException("Table definition not found with ID: " + id));
    }

    @Transactional
    public void deleteTable(Long id) {
        tableDefinitionRepository.findById(id).ifPresent(tableMetadata -> {
            String dropTableSql = ddlGenerator.generateDropTableSql(tableMetadata.getTableName());
            jdbcTemplate.execute(dropTableSql); // Drop the actual table
            tableDefinitionRepository.delete(tableMetadata); // Delete metadata
        });
    }

    public List<TableMetadata> getAllTableDefinitions() {
        return tableDefinitionRepository.findAll();
    }

    public Optional<TableMetadata> getTableDefinitionById(Long id) {
        return tableDefinitionRepository.findById(id);
    }

    public Optional<TableMetadata> findByTableName(String tableName) {
    //Anil
        return Optional.empty();
    }
}