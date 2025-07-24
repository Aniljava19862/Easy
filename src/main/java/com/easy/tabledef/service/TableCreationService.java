package com.easy.tabledef.service;

import com.easy.tabledef.dto.ColumnDefinitionDto;
import com.easy.tabledef.dto.TableDefinitionDto;
import com.easy.tabledef.dto.TableRecordDto;
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
import java.util.UUID;
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

    // --- NEW: Define your internal UUID column name ---
    private static final String SYSTEM_UUID_COLUMN_NAME = "system_row_id";
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
    /**
     * Endpoint to create a new dynamic table and save its definition.
     * POST /api/tables/create
     *
     * @param tableDefinition The JSON request body containing table and column definitions.
     * @return ResponseEntity indicating success or failure.
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
        // This ensures the tableDefinition object has its ID set if it's new
        TableDefinition savedTableDef = tableDefinitionRepository.save(tableDefinition);
        System.out.println("Table metadata saved with ID: " + savedTableDef.getId());

        // Build CREATE TABLE SQL
        StringBuilder createPhysicalTableSql = new StringBuilder("CREATE TABLE ");
        createPhysicalTableSql.append(sanitizeSqlIdentifier(physicalTableName)).append(" (");

        // --- NEW: Add the system-generated UUID column as the actual PRIMARY KEY for your internal tracking ---
        // This column's name should be consistent (SYSTEM_UUID_COLUMN_NAME)
        // Use VARCHAR(36) for storing UUIDs as strings (standard representation)
        // This makes it the technical primary key of the table in the database
        createPhysicalTableSql.append(String.format("%s VARCHAR(36) PRIMARY KEY", SYSTEM_UUID_COLUMN_NAME));


        // Add user-defined columns, ensuring they don't override the primary key definition
        List<String> columnSqlDefinitions = tableDefinition.getColumns().stream()
                .map(col -> {
                    StringBuilder colDefBuilder = new StringBuilder();
                    colDefBuilder.append(sanitizeSqlIdentifier(col.getColumnName())).append(" ");
                    colDefBuilder.append(mapColumnTypeToSql(col.getColumnType()));
                    if (col.isPrimaryKey()) {
                        // If the user marked a column as primary key, make it UNIQUE in the DB.
                        // The actual DB PRIMARY KEY is now SYSTEM_UUID_COLUMN_NAME.
                        colDefBuilder.append(" UNIQUE");
                    }
                    // Add NOT NULL if desired for user-defined PKs or other required columns
                    // For now, we'll keep it simple as specified in your original model
                    return colDefBuilder.toString();
                })
                .collect(Collectors.toList());

        // Only append user-defined columns if there are any, otherwise close the table creation with just the UUID PK
        if (!columnSqlDefinitions.isEmpty()) {
            createPhysicalTableSql.append(", "); // Add a comma after the UUID PK if user columns exist
            createPhysicalTableSql.append(String.join(", ", columnSqlDefinitions));
        }

        createPhysicalTableSql.append(")");

        try {
            System.out.println("Executing CREATE TABLE SQL for physical table: " + createPhysicalTableSql.toString());
            jdbcTemplate.execute(createPhysicalTableSql.toString());
            System.out.println("Physical table '" + physicalTableName + "' created successfully.");

            // Create indexes for columns marked createIndex=true by the user
            // You don't need to create an index for the SYSTEM_UUID_COLUMN_NAME as it's the PRIMARY KEY (automatically indexed)
            for (ColumnDefinition column : tableDefinition.getColumns()) {
                if (column.isCreateIndex() && !column.isPrimaryKey()) { // Primary key is already indexed
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
            // It's good practice to delete the metadata if physical table creation fails
            // This rollback will be handled by @Transactional if the exception propagates
            // But an explicit delete here ensures consistency if @Transactional somehow fails to revert (unlikely)
            tableDefinitionRepository.delete(savedTableDef); // Delete the saved metadata
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
        // Generate and add the system-generated UUID to the data map
        String rowUuid = UUID.randomUUID().toString();
        data.put(SYSTEM_UUID_COLUMN_NAME, rowUuid);
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

    // --- New Method for Single Row Retrieval ---
    /**
     * Retrieves a single row from a dynamically created table based on a logical table name
     * and a specific filter column/value.
     *
     * @param logicalTableName The logical name of the table.
     * @param filterColumn     The column name to use for filtering (e.g., "id", "username").
     * @param filterValue      The value to match in the filterColumn.
     * @return An Optional containing a Map<String, Object> representing the row, or empty if not found.
     * @throws IllegalArgumentException if table definition or filter column is invalid.
     */
    @Transactional(readOnly = true)
    public Optional<Map<String, Object>> getSingleRowFromDynamicTable(
            String logicalTableName, String filterColumn, String filterValue) {

        // 1. Get TableDefinition to find the physical table name and column details
        Optional<TableDefinition> tableDefOptional = tableDefinitionRepository.findByTableName(logicalTableName);

        if (tableDefOptional.isEmpty()) {
            throw new IllegalArgumentException("Table definition with logical name '" + logicalTableName + "' not found.");
        }

        TableDefinition tableDefinition = tableDefOptional.get();
        String finalTableName = tableDefinition.getFinalTableName();

        // Ensure columns are loaded if fetching individual TableDefinition entities
        // for other purposes or if this service method is reused for entities.
        // For this specific method, we only need basic metadata from tableDefinition.
        // If TableDefinitionDto was used instead, this wouldn't be necessary here.

        // 2. Validate filterColumn against actual table columns (optional but recommended)
        // If columns are LAZY, you need to initialize them here if you want to validate against them.
        // Or you can rely on the DB query failing if the column doesn't exist.
        // For robustness, initialize and validate:
        Hibernate.initialize(tableDefinition.getColumns()); // Ensure columns are loaded
        boolean columnExists = tableDefinition.getColumns().stream()
                .anyMatch(col -> col.getColumnName().equalsIgnoreCase(filterColumn));

        if (!columnExists) {
            throw new IllegalArgumentException("Column '" + filterColumn + "' does not exist in table '" + logicalTableName + "'.");
        }

        // 3. Construct and execute the SQL query using DynamicTableAccessor
        // Sanitize identifiers to prevent SQL injection
        String sanitizedFinalTableName = sanitizeSqlIdentifier(finalTableName);
        String sanitizedFilterColumn = sanitizeSqlIdentifier(filterColumn);

        String sql = String.format("SELECT * FROM %s WHERE %s = ?", sanitizedFinalTableName, sanitizedFilterColumn);

        try {
            // queryForList returns a list, we need the first (and only) element
            List<Map<String, Object>> result = jdbcTemplate.queryForList(sql, filterValue);
            return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            // This exception is usually thrown if queryForObject is used and no result found.
            // queryForList returns empty list, so this catch might not be strictly necessary for queryForList.
            return Optional.empty();
        } catch (Exception e) {
            // Log other potential SQL errors (e.g., column not found, type mismatch in WHERE clause)
            throw new RuntimeException("Error retrieving single row from dynamic table '" + logicalTableName + "': " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public Optional<Map<String, Object>> getSingleRowBySystemIdFromDynamicTable(
            String logicalTableName, String systemRowId) {

        Optional<TableDefinition> tableDefOptional = tableDefinitionRepository.findByTableName(logicalTableName);

        if (tableDefOptional.isEmpty()) {
            throw new IllegalArgumentException("Table definition with logical name '" + logicalTableName + "' not found.");
        }

        TableDefinition tableDefinition = tableDefOptional.get();
        String finalTableName = tableDefinition.getFinalTableName();

        // --- DELEGATE TO DYNAMIC TABLE ACCESSOR FOR THE ACTUAL SELECTION ---
        // You'll need to add a selectOne method to DynamicTableAccessor
        return dynamicTableAccessor.selectOne(finalTableName, SYSTEM_UUID_COLUMN_NAME, systemRowId);
    }

    // --- NEW METHOD: Get Combined Table Metadata and Single Row Data ---
    /**
     * Retrieves the table definition (column metadata) and a specific row's data
     * for a dynamic table based on its logical name and the system-generated row ID.
     *
     * @param logicalTableName The logical name of the table.
     * @param systemRowId      The system-generated UUID of the row to fetch.
     * @return An Optional containing a TableRecordDto with column definitions and row data,
     * or empty if the table or row is not found.
     * @throws IllegalArgumentException if the logicalTableName is not found.
     * @throws RuntimeException if there's a database access error.
     */
    @Transactional(readOnly = true)
    public Optional<TableRecordDto> getTableDefinitionAndSingleRow(
            String logicalTableName, String systemRowId) {

        // 1. Fetch Table Definition (metadata)
        Optional<TableDefinition> tableDefOptional = tableDefinitionRepository.findByTableName(logicalTableName);
        if (tableDefOptional.isEmpty()) {
            throw new IllegalArgumentException("Table definition for logical name '" + logicalTableName + "' not found.");
        }
        TableDefinition tableDefinition = tableDefOptional.get();

        // Ensure columns are initialized before converting to DTOs
        if (tableDefinition.getColumns() != null) {
            Hibernate.initialize(tableDefinition.getColumns());
        }

        // Convert ColumnDefinition entities to ColumnDefinitionDto
        List<ColumnDefinitionDto> columnDefinitionsDto = tableDefinition.getColumns().stream()
                .map(ColumnDefinitionDto::fromEntity)
                .collect(Collectors.toList());

        // 2. Fetch the specific row data using the system_row_id
        // This method should be in DynamicTableAccessor or be a call to it.
        // Assuming selectOne method exists in DynamicTableAccessor:
        String finalTableName = tableDefinition.getFinalTableName();
        Optional<Map<String, Object>> rowDataOptional =
                dynamicTableAccessor.selectOne(finalTableName, SYSTEM_UUID_COLUMN_NAME, systemRowId);

        if (rowDataOptional.isEmpty()) {
            // Table definition found, but specific row not found.
            return Optional.empty();
        }

        Map<String, Object> rowData = rowDataOptional.get();

        // 3. Construct the combined DTO
        TableRecordDto tableRecordDto = new TableRecordDto(columnDefinitionsDto, rowData);

        return Optional.of(tableRecordDto);
    }


}