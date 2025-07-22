package com.easy.tabledef.service;

import com.easy.tabledef.dto.TableDefinitionDto;
import com.easy.tabledef.model.ColumnDefinition;
import com.easy.tabledef.model.TableDefinition;
import com.easy.tabledef.repository.TableDefinitionRepository;
import com.easy.tabledef.util.DynamicTableAccessor; // Import the DynamicTableAccessor component
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class TableCreationService {

    @Autowired
    private JdbcTemplate jdbcTemplate; // Used for CREATE TABLE, ALTER TABLE, etc.

    @Autowired
    private TableDefinitionRepository tableDefinitionRepository; // Used for managing table metadata

    @Autowired
    private DynamicTableAccessor dynamicTableAccessor; // <--- Auto-wired as a Spring component

    // Pattern to validate SQL identifiers. Used internally for safety.
    private static final Pattern SQL_IDENTIFIER_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");

    /**
     * Helper method to sanitize SQL identifiers to prevent injection.
     * Only allows alphanumeric characters and underscores, starting with letter or underscore.
     */
    private String sanitizeSqlIdentifier(String name) {
        if (name == null || !SQL_IDENTIFIER_PATTERN.matcher(name).matches()) {
            throw new IllegalArgumentException("Invalid SQL identifier: '" + name + "'. Must be alphanumeric and start with a letter or underscore.");
        }
        return name;
    }

    /**
     * Maps a generic column type (from ColumnDefinition) to a database-specific SQL type.
     * This method would need to be enhanced for full database portability if types differ significantly.
     */
    private String mapColumnTypeToSql(String columnType) {
        return switch (columnType.toLowerCase()) {
            case "currency", "decimal" -> "DECIMAL(19, 4)";
            case "text" -> "VARCHAR(255)";
            case "longtext" -> "CLOB"; // Might be TEXT (PostgreSQL) or LONGTEXT (MySQL)
            case "number" -> "BIGINT";
            case "boolean" -> "BOOLEAN"; // Might be TINYINT(1) (MySQL) or NUMBER(1) (Oracle)
            case "date" -> "DATE";
            case "datetime" -> "TIMESTAMP";
            default -> "VARCHAR(255)"; // Default if type is unknown
        };
    }

    /**
     * Creates a new dynamic table in the database and persists its metadata.
     *
     * @param tableDefinition The definition of the table to create.
     * @return A message indicating the result of the operation.
     * @throws IllegalArgumentException If table definition is invalid or already exists.
     * @throws RuntimeException         If the physical table creation fails.
     */
    @Transactional
    public String createTable(TableDefinition tableDefinition) {
        String logicalTableName = tableDefinition.getTableName();
        String physicalTableName = tableDefinition.getFinalTableName(); // This should be pre-populated/derived

        // Basic validation
        if (tableDefinition.getColumns() == null || tableDefinition.getColumns().isEmpty()) {
            throw new IllegalArgumentException("Table definition must have at least one column.");
        }
        if (physicalTableName == null || physicalTableName.trim().isEmpty()) {
            throw new IllegalArgumentException("Final table name cannot be null or empty.");
        }

        // Check for existing logical/physical table names
        if (tableDefinitionRepository.findByTableName(logicalTableName).isPresent()) {
            throw new IllegalArgumentException("A table definition with logical name '" + logicalTableName + "' already exists.");
        }
        if (tableDefinitionRepository.findByFinalTableName(physicalTableName).isPresent()) {
            throw new IllegalArgumentException("A table definition already uses the physical table name '" + physicalTableName + "'.");
        }

        // Set timestamps and link columns to table definition
        tableDefinition.setCreatedAt(LocalDateTime.now());
        tableDefinition.setUpdatedAt(LocalDateTime.now());
        tableDefinition.getColumns().forEach(column -> column.setTableDefinition(tableDefinition));

        // Save metadata first (in case physical table creation fails)
        TableDefinition savedTableDef = tableDefinitionRepository.save(tableDefinition);
        System.out.println("Table metadata saved with ID: " + savedTableDef.getId());

        // Build CREATE TABLE SQL
        StringBuilder createPhysicalTableSql = new StringBuilder("CREATE TABLE ");
        createPhysicalTableSql.append(sanitizeSqlIdentifier(physicalTableName)).append(" (");

        List<String> columnSqlDefinitions = tableDefinition.getColumns().stream()
                .map(col -> {
                    StringBuilder colDefBuilder = new StringBuilder();
                    colDefBuilder.append(sanitizeSqlIdentifier(col.getColumnName())).append(" ");
                    colDefBuilder.append(mapColumnTypeToSql(col.getColumnType()));
                    if (col.isPrimaryKey()) {
                        colDefBuilder.append(" PRIMARY KEY"); // Assumes auto-incrementing for PKs if not specified
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

            // Create indexes
            for (ColumnDefinition column : tableDefinition.getColumns()) {
                if (column.isCreateIndex() && !column.isPrimaryKey()) { // Don't create index on PK, it's usually automatic
                    String createIndexSql = "CREATE INDEX " + sanitizeSqlIdentifier(physicalTableName + "_" + column.getColumnName() + "_idx")
                            + " ON " + sanitizeSqlIdentifier(physicalTableName) + "(" + sanitizeSqlIdentifier(column.getColumnName()) + ")";
                    System.out.println("Executing CREATE INDEX SQL: " + createIndexSql);
                    jdbcTemplate.execute(createIndexSql);
                    System.out.println("Index for column '" + column.getColumnName() + "' created successfully.");
                }
            }

            return "Table '" + physicalTableName + "' created successfully. Metadata saved and physical table created.";

        } catch (Exception e) {
            System.err.println("Error creating physical table '" + physicalTableName + "': " + e.getMessage());
            // Rollback metadata save if physical table creation fails (due to @Transactional)
            throw new RuntimeException("Failed to create physical table: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves a table definition by its logical name.
     */
    @Transactional(readOnly = true)
    public Optional<TableDefinition> getTableDefinitionByLogicalName(String tableName) {
        return tableDefinitionRepository.findByTableName(tableName);
    }

    /**
     * Retrieves a table definition by its physical name.
     */
    @Transactional(readOnly = true)
    public Optional<TableDefinition> getTableDefinitionByPhysicalName(String finalTableName) {
        return tableDefinitionRepository.findByFinalTableName(finalTableName);
    }

    // --- Dynamic Table Data Operations (using DynamicTableAccessor) ---

    /**
     * Adds data to a dynamic table identified by its logical name.
     * Uses DynamicTableAccessor for actual database interaction.
     */
    @Transactional
    public int addDataToDynamicTable(String logicalTableName, Map<String, Object> data) {
        TableDefinition tableDef = tableDefinitionRepository.findByTableName(logicalTableName)
                .orElseThrow(() -> new IllegalArgumentException("Table definition not found for logical name: " + logicalTableName));
        String finalTableName = tableDef.getFinalTableName();
        return dynamicTableAccessor.insert(finalTableName, data); // Delegate to DynamicTableAccessor
    }

    /**
     * Retrieves all data from a dynamic table identified by its logical name.
     * Uses DynamicTableAccessor for actual database interaction.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllDataFromDynamicTable(String logicalTableName) {
        TableDefinition tableDef = tableDefinitionRepository.findByTableName(logicalTableName)
                .orElseThrow(() -> new IllegalArgumentException("Table definition not found for logical name: " + logicalTableName));
        String finalTableName = tableDef.getFinalTableName();
        return dynamicTableAccessor.readAll(finalTableName); // Delegate
    }

    /**
     * Retrieves filtered data from a dynamic table identified by its logical name.
     * Uses DynamicTableAccessor for actual database interaction.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getFilteredDataFromDynamicTable(String logicalTableName, String filterColumn, Object filterValue) {
        TableDefinition tableDef = tableDefinitionRepository.findByTableName(logicalTableName)
                .orElseThrow(() -> new IllegalArgumentException("Table definition not found for logical name: " + logicalTableName));
        String finalTableName = tableDef.getFinalTableName();
        // Note: filterValue comes as String from controller, if the actual column type is different (e.g., Number),
        // you might need to convert it here based on columnDefinition metadata.
        // For simplicity, passing as Object and relying on JdbcTemplate's type conversion.
        return dynamicTableAccessor.readByFilter(finalTableName, filterColumn, filterValue); // Delegate
    }

    /**
     * Updates data in a dynamic table identified by its logical name.
     * Uses DynamicTableAccessor for actual database interaction.
     */
    @Transactional
    public int updateDataInDynamicTable(String logicalTableName, Map<String, Object> updateData, String filterColumn, Object filterValue) {
        TableDefinition tableDef = tableDefinitionRepository.findByTableName(logicalTableName)
                .orElseThrow(() -> new IllegalArgumentException("Table definition not found for logical name: " + logicalTableName));
        String finalTableName = tableDef.getFinalTableName();
        // Similar to readByFilter, consider type conversion for filterValue if necessary.
        return dynamicTableAccessor.update(finalTableName, updateData, filterColumn, filterValue); // Delegate
    }

    /**
     * Deletes data from a dynamic table identified by its logical name.
     * Uses DynamicTableAccessor for actual database interaction.
     */
    @Transactional
    public int deleteDataFromDynamicTable(String logicalTableName, String filterColumn, Object filterValue) {
        TableDefinition tableDef = tableDefinitionRepository.findByTableName(logicalTableName)
                .orElseThrow(() -> new IllegalArgumentException("Table definition not found for logical name: " + logicalTableName));
        String finalTableName = tableDef.getFinalTableName();
        // Similar to readByFilter, consider type conversion for filterValue if necessary.
        return dynamicTableAccessor.delete(finalTableName, filterColumn, filterValue); // Delegate
    }

    @Transactional(readOnly = true)
    public List<TableDefinitionDto> getAllTableDefinitions() { // <-- Returns DTO list
        List<TableDefinition> entities = tableDefinitionRepository.findAll();
        // Explicitly initialize the lazy collection for each entity within the active session
        // This makes sure 'columns' are loaded from DB before session closes
        entities.forEach(tableDef -> {
            if (tableDef.getColumns() != null) { // Defensive check
                Hibernate.initialize(tableDef.getColumns());
            }
        });
        // Map entities to DTOs
        return entities.stream()
                .map(TableDefinitionDto::fromEntity) // <--- Uses your DTO factory method
                .collect(Collectors.toList());
    }
}