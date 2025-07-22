package com.easy.tabledef.service;

import com.easy.tabledef.model.ColumnDefinition;
import com.easy.tabledef.model.TableDefinition;
import com.easy.tabledef.repository.TableDefinitionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class TableCreationService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TableDefinitionRepository tableDefinitionRepository;

    private static final Pattern SQL_IDENTIFIER_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");

    private String sanitizeSqlIdentifier(String name) {
        if (name == null || !SQL_IDENTIFIER_PATTERN.matcher(name).matches()) {
            throw new IllegalArgumentException("Invalid SQL identifier: '" + name + "'. Must be alphanumeric, start with a letter or underscore.");
        }
        return name;
    }

    private String mapColumnTypeToSql(String columnType) {
        return switch (columnType.toLowerCase()) {
            case "currency" -> "DECIMAL(19, 4)";
            case "text" -> "VARCHAR(255)";
            case "longtext" -> "CLOB";
            case "number" -> "BIGINT";
            case "decimal" -> "DECIMAL(10, 2)";
            case "boolean" -> "BOOLEAN";
            case "date" -> "DATE";
            case "datetime" -> "TIMESTAMP";
            default -> "VARCHAR(255)";
        };
    }

    @Transactional
    public String createTable(TableDefinition tableDefinition) {
        String logicalTableName = tableDefinition.getTableName();
        String physicalTableName = sanitizeSqlIdentifier(tableDefinition.getFinalTableName());

        if (tableDefinition.getColumns() == null || tableDefinition.getColumns().isEmpty()) {
            throw new IllegalArgumentException("Table definition must have at least one column.");
        }

        if (tableDefinitionRepository.findByTableName(logicalTableName).isPresent()) {
            throw new IllegalArgumentException("A table definition with logical name '" + logicalTableName + "' already exists.");
        }

        if (tableDefinitionRepository.findByFinalTableName(physicalTableName).isPresent()) {
            throw new IllegalArgumentException("A table definition already uses the physical table name '" + physicalTableName + "'.");
        }

        tableDefinition.setCreatedAt(LocalDateTime.now());
        tableDefinition.setUpdatedAt(LocalDateTime.now());

        // Set the bidirectional relationship
        tableDefinition.getColumns().forEach(column -> {
            column.setTableDefinition(tableDefinition);
            sanitizeSqlIdentifier(column.getColumnName());
        });

        TableDefinition savedTableDef = tableDefinitionRepository.save(tableDefinition);
        System.out.println("Table metadata saved with ID: " + savedTableDef.getId());

        StringBuilder createPhysicalTableSql = new StringBuilder("CREATE TABLE ");
        createPhysicalTableSql.append(physicalTableName).append(" (");

        List<String> columnSqlDefinitions = tableDefinition.getColumns().stream()
                .map(col -> {
                    StringBuilder colDefBuilder = new StringBuilder();
                    colDefBuilder.append(sanitizeSqlIdentifier(col.getColumnName())).append(" ");
                    colDefBuilder.append(mapColumnTypeToSql(col.getColumnType()));
                    if (col.isPrimaryKey()) {
                        colDefBuilder.append(" PRIMARY KEY");
                    }
                    return colDefBuilder.toString();
                })
                .collect(Collectors.toList());

        createPhysicalTableSql.append(String.join(", ", columnSqlDefinitions));
        createPhysicalTableSql.append(")");

        try {
            System.out.println("Executing CREATE TABLE SQL for physical table: " + createPhysicalTableSql.toString());
            jdbcTemplate.execute(createPhysicalTableSql.toString());
            System.out.println("Physical table '" + physicalTableName + "' created successfully.");

            for (ColumnDefinition column : tableDefinition.getColumns()) {
                if (column.isCreateIndex() && !column.isPrimaryKey()) {
                    String createIndexSql = "CREATE INDEX " + sanitizeSqlIdentifier(physicalTableName + "_" + column.getColumnName() + "_idx")
                            + " ON " + physicalTableName + "(" + sanitizeSqlIdentifier(column.getColumnName()) + ")";
                    System.out.println("Executing CREATE INDEX SQL: " + createIndexSql);
                    jdbcTemplate.execute(createIndexSql);
                    System.out.println("Index for column '" + column.getColumnName() + "' created successfully.");
                }
            }

            return "Table '" + physicalTableName + "' created successfully. Metadata saved and physical table created.";

        } catch (Exception e) {
            System.err.println("Error creating physical table '" + physicalTableName + "': " + e.getMessage());
            throw new RuntimeException("Failed to create physical table: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public Optional<TableDefinition> getTableDefinitionByLogicalName(String tableName) {
        return tableDefinitionRepository.findByTableName(tableName);
    }

    @Transactional(readOnly = true)
    public Optional<TableDefinition> getTableDefinitionByPhysicalName(String finalTableName) {
        return tableDefinitionRepository.findByFinalTableName(finalTableName);
    }
}