package com.easy.tabledef.service;

import com.easy.application.dbtest.data.DatabaseConnectionDetails;
import com.easy.application.dbtest.service.DatabaseConnectivityService;
import com.easy.database.DynamicDataSourceManager;
import com.easy.projectconfig.model.ProjectConfig;
import com.easy.projectconfig.service.ProjectConfigService;
import com.easy.tabledef.dto.ColumnDefinitionDto;
import com.easy.tabledef.dto.TableDataResponseDto;
import com.easy.tabledef.dto.TableDefinitionDto;
import com.easy.tabledef.model.ColumnDefinition;
import com.easy.tabledef.model.TableDefinition;
import com.easy.tabledef.repository.ColumnDefinitionRepository;
import com.easy.tabledef.repository.TableDefinitionRepository;
import com.easy.tabledef.util.DynamicTableAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TableCreationService {

    private static final String SYSTEM_UUID_COLUMN_NAME = "system_row_id";

    @Autowired
    private TableDefinitionRepository tableDefinitionRepository;

    @Autowired
    private ColumnDefinitionRepository columnDefinitionRepository;

    @Autowired
    private DynamicDataSourceManager dynamicDataSourceManager;

    @Autowired
    private DatabaseConnectivityService databaseConnectivityService;

    @Autowired
    private ProjectConfigService projectConfigService;

    @Autowired
    private DynamicTableAccessor dynamicTableAccessor;


    /**
     * Creates a new dynamic table in the database associated with a project,
     * and saves its definition metadata.
     *
     * @param tableDefinition The definition of the table to create.
     * @param projectConfigId The UUID of the project to which this table belongs.
     * @return The DTO of the created table definition.
     * @throws IllegalArgumentException if table name already exists or invalid definitions.
     * @throws RuntimeException if table creation in target DB fails.
     */
    @Transactional
    public TableDefinitionDto createTable(TableDefinition tableDefinition, String projectConfigId) {
        // 1. Validate projectConfigId and get DatabaseConnectionDetails
        ProjectConfig projectConfig = projectConfigService.getProjectConfigById(projectConfigId)
                .orElseThrow(() -> new IllegalArgumentException("Project configuration not found for ID: " + projectConfigId));
        DatabaseConnectionDetails dbDetails = databaseConnectivityService.getSavedConnectionByUuid(projectConfig.getDatabaseConnectionIdRef())
                .orElseThrow(() -> new IllegalStateException("Database connection details not found for project ID: " + projectConfigId));

        // 2. Ensure the table name is unique within the project
        if (tableDefinitionRepository.findByTableNameAndProjectConfigIdRef(tableDefinition.getTableName(), projectConfigId).isPresent()) {
            throw new IllegalArgumentException("Table with logical name '" + tableDefinition.getTableName() + "' already exists for project '" + projectConfig.getProjectName() + "'.");
        }

        // 3. Generate finalTableName for the physical database
        String generatedFinalTableName = tableDefinition.getTableName().toLowerCase()
                + (tableDefinition.getAppSuffix() != null && !tableDefinition.getAppSuffix().isEmpty() ? "_" + tableDefinition.getAppSuffix().toLowerCase() : "")
                + "_" + UUID.randomUUID().toString().substring(0, 8);

        tableDefinition.setFinalTableName(generatedFinalTableName);
        tableDefinition.setProjectConfigIdRef(projectConfigId);
        tableDefinition.setCreatedAt(LocalDateTime.now());
        tableDefinition.setUpdatedAt(LocalDateTime.now());

        List<ColumnDefinition> columnDefinitions = tableDefinition.getColumns();
        if (columnDefinitions == null || columnDefinitions.isEmpty()) {
            throw new IllegalArgumentException("Table must have at least one column defined.");
        }

        List<String> indexSqlStatements = new ArrayList<>();

        // 4. Build the SQL CREATE TABLE statement
        StringBuilder createTableSql = new StringBuilder("CREATE TABLE `") // Quote table name
                .append(tableDefinition.getFinalTableName())
                .append("` (");

        // Add the system_row_id column (primary key for data rows)
        createTableSql.append("`").append(SYSTEM_UUID_COLUMN_NAME).append("` VARCHAR(36) PRIMARY KEY"); // Quote system_row_id

        // Process other columns
        for (ColumnDefinition column : columnDefinitions) {
            column.setTableDefinition(tableDefinition);
            column.setCreatedAt(LocalDateTime.now());
            column.setUpdatedAt(LocalDateTime.now());

            // --- Handle reference columns ---
            if (column.isReference()) {
                validateReferenceColumnMetadata(column, projectConfigId);
                createTableSql.append(", `").append(column.getColumnName()).append("` VARCHAR(36)"); // Quote column name

                // Add index creation statement for reference columns for faster lookups/joins
                indexSqlStatements.add(
                        String.format("CREATE INDEX `idx_%s_%s` ON `%s` (`%s`)", // Quote index, table, and column names
                                tableDefinition.getFinalTableName(),
                                column.getColumnName(),
                                tableDefinition.getFinalTableName(),
                                column.getColumnName())
                );
            } else {
                String sqlType = mapColumnTypeToSql(column.getColumnType());
                createTableSql.append(", `").append(column.getColumnName()).append("` ").append(sqlType); // Quote column name
            }
            // --- End reference column handling ---

            // Add constraints
            if (!column.isNullable()) {
                createTableSql.append(" NOT NULL");
            }
            if (column.isUnique() && !column.isPrimaryKey()) {
                createTableSql.append(" UNIQUE");
                // Also add an index for unique columns if not primary key
                indexSqlStatements.add(
                        String.format("CREATE UNIQUE INDEX `idx_unique_%s_%s` ON `%s` (`%s`)", // Quote all parts
                                tableDefinition.getFinalTableName(),
                                column.getColumnName(),
                                tableDefinition.getFinalTableName(),
                                column.getColumnName())
                );
            }
            if (column.getDefaultValue() != null && !column.getDefaultValue().isEmpty()) {
                String sqlTypeUsed = column.isReference() ? "VARCHAR(36)" : mapColumnTypeToSql(column.getColumnType());
                if (sqlTypeUsed.startsWith("VARCHAR") || sqlTypeUsed.startsWith("TEXT") || sqlTypeUsed.startsWith("DATE") || sqlTypeUsed.startsWith("DATETIME")) {
                    createTableSql.append(" DEFAULT '").append(column.getDefaultValue()).append("'");
                } else {
                    createTableSql.append(" DEFAULT ").append(column.getDefaultValue());
                }
            }
        }
        createTableSql.append(")");

        // 5. Get the JdbcTemplate for the project's database
        JdbcTemplate jdbcTemplate = dynamicDataSourceManager.getJdbcTemplate(dbDetails);

        // 6. Execute CREATE TABLE and then CREATE INDEX statements
        try {
            jdbcTemplate.execute(createTableSql.toString());
            System.out.println("Created dynamic table: " + tableDefinition.getFinalTableName() + " for project: " + projectConfig.getProjectName());

            for (String indexSql : indexSqlStatements) {
                try {
                    jdbcTemplate.execute(indexSql);
                    System.out.println("Created index: " + indexSql);
                } catch (Exception e) {
                    System.err.println("Warning: Failed to create index '" + indexSql + "': " + e.getMessage());
                    // Don't throw fatal error for index creation if table is already created
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to create table in target database: " + e.getMessage(), e);
        }

        // 7. Save metadata to your application's database
        TableDefinition savedTableDefinition = tableDefinitionRepository.save(tableDefinition);
        columnDefinitionRepository.saveAll(columnDefinitions);

        return TableDefinitionDto.fromEntity(savedTableDefinition);
    }


    /**
     * Updates an existing dynamic table definition and its columns.
     * Note: This method currently only updates the metadata in the application's database.
     * It does NOT generate ALTER TABLE statements on the dynamic database.
     * For full functionality, you would need to implement physical schema alteration here.
     *
     * @param id The UUID of the TableDefinition to update.
     * @param updatedDefinition The updated table definition.
     * @param projectConfigId The UUID of the project.
     * @return The DTO of the updated table definition.
     */
    @Transactional
    public TableDefinitionDto updateTableDefinition(String id, TableDefinition updatedDefinition, String projectConfigId) {
        TableDefinition existingTableDefinition = tableDefinitionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Table definition not found with ID: " + id));

        if (!existingTableDefinition.getProjectConfigIdRef().equals(projectConfigId)) {
            throw new IllegalArgumentException("Table with ID '" + id + "' does not belong to project '" + projectConfigId + "'.");
        }

        existingTableDefinition.setTableName(updatedDefinition.getTableName());
        existingTableDefinition.setAppSuffix(updatedDefinition.getAppSuffix());
        existingTableDefinition.setDescription(updatedDefinition.getDescription());
        existingTableDefinition.setUpdatedAt(LocalDateTime.now());

        // Handle columns: For now, we clear and re-add for simplicity in metadata.
        // For physical DB schema updates, this needs to be much more complex (ALTER TABLE ADD/DROP/MODIFY COLUMN).
        columnDefinitionRepository.deleteByTableDefinition(existingTableDefinition); // Delete old columns associated with this table
        existingTableDefinition.getColumns().clear(); // Clear the collection to ensure JPA recognizes changes

        List<ColumnDefinition> newColumnDefinitions = updatedDefinition.getColumns();
        if (newColumnDefinitions != null) {
            for (ColumnDefinition column : newColumnDefinitions) {
                column.setTableDefinition(existingTableDefinition);
                column.setCreatedAt(LocalDateTime.now());
                column.setUpdatedAt(LocalDateTime.now());

                if (column.isReference()) {
                    validateReferenceColumnMetadata(column, projectConfigId);
                }
            }
            existingTableDefinition.setColumns(newColumnDefinitions); // Set the new collection
        }

        TableDefinition savedTableDefinition = tableDefinitionRepository.save(existingTableDefinition);
        return TableDefinitionDto.fromEntity(savedTableDefinition);
    }


    /**
     * Validates the metadata for a reference column during table definition.
     * Ensures referenced table and column exist and are valid targets.
     */
    private void validateReferenceColumnMetadata(ColumnDefinition column, String projectConfigId) {
        if (column.getReferencedTableIdRef() == null || column.getReferencedTableIdRef().isEmpty()) {
            throw new IllegalArgumentException("Reference column '" + column.getColumnName() + "' must specify a 'referencedTableIdRef'.");
        }
        if (column.getReferencedColumnIdRef() == null || column.getReferencedColumnIdRef().isEmpty()) {
            throw new IllegalArgumentException("Reference column '" + column.getColumnName() + "' must specify a 'referencedColumnIdRef'.");
        }

        TableDefinition referencedTableDef = tableDefinitionRepository
                .findById(column.getReferencedTableIdRef())
                .orElseThrow(() -> new IllegalArgumentException("Referenced table with ID '" + column.getReferencedTableIdRef() + "' not found for column '" + column.getColumnName() + "'."));

        if (!referencedTableDef.getProjectConfigIdRef().equals(projectConfigId)) {
            throw new IllegalArgumentException("Referenced table '" + referencedTableDef.getTableName() + "' (ID: " + referencedTableDef.getId() + ") does not belong to the same project as the current table. Cross-project references are not allowed.");
        }

        ColumnDefinition targetColumn = referencedTableDef.getColumns().stream()
                .filter(c -> c.getId().equals(column.getReferencedColumnIdRef()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Referenced column with ID '" + column.getReferencedColumnIdRef() + "' not found in table '" + referencedTableDef.getTableName() + "'."));

        if (!targetColumn.isPrimaryKey() && !targetColumn.isUnique()) {
            throw new IllegalArgumentException("Referenced column '" + targetColumn.getColumnName() + "' in table '" + referencedTableDef.getTableName() + "' (UUID: " + targetColumn.getId() + ") must be a Primary Key or Unique column to be referenced.");
        }

        column.setReferencedTableLogicalName(referencedTableDef.getTableName());
        column.setReferencedColumnLogicalName(targetColumn.getColumnName());
    }

    /**
     * Adds data to a dynamic table. Performs validation for reference columns.
     *
     * @param logicalTableName The logical name of the target table.
     * @param projectConfigId The UUID of the project.
     * @param data A map of column names to their values.
     * @return The number of rows affected (should be 1).
     * @throws IllegalArgumentException if data is invalid or reference integrity is violated.
     * @throws RuntimeException if database operation fails.
     */
    @Transactional
    public int addDataToDynamicTable(String logicalTableName, String projectConfigId, Map<String, Object> data) {
        JdbcTemplate jdbcTemplate = getJdbcTemplateForProject(projectConfigId);
        TableDefinition tableDef = getTableDefinitionByLogicalNameAndProject(logicalTableName, projectConfigId)
                .orElseThrow(() -> new IllegalArgumentException("Table definition not found for logical name: " + logicalTableName + " in project: " + projectConfigId));

        for (ColumnDefinition colDef : tableDef.getColumns()) {
            if (colDef.isReference()) {
                String referencingColumnName = colDef.getColumnName();
                Object providedValue = data.get(referencingColumnName);

                if (providedValue == null) {
                    if (!colDef.isNullable()) {
                        throw new IllegalArgumentException("Non-nullable reference column '" + referencingColumnName + "' cannot be null.");
                    }
                    continue;
                }

                if (!(providedValue instanceof String)) {
                    throw new IllegalArgumentException("Value for reference column '" + referencingColumnName + "' must be a String (UUID). Provided: " + providedValue.getClass().getSimpleName());
                }

                boolean exists = checkReferencedValueExists(
                        jdbcTemplate,
                        colDef.getReferencedTableIdRef(),
                        colDef.getReferencedColumnIdRef(),
                        (String) providedValue,
                        projectConfigId
                );
                if (!exists) {
                    throw new IllegalArgumentException(
                            "Invalid reference: Value '" + providedValue + "' for column '" + referencingColumnName +
                                    "' does not exist in the referenced table '" + colDef.getReferencedTableLogicalName() +
                                    "' (column: " + colDef.getReferencedColumnLogicalName() + ")."
                    );
                }
            }
        }

        String rowUuid = UUID.randomUUID().toString();
        data.put(SYSTEM_UUID_COLUMN_NAME, rowUuid);

        return dynamicTableAccessor.insert(jdbcTemplate, tableDef.getFinalTableName(), data);
    }



    /**
     * Retrieves a single row by its system_row_id from a dynamic table and optionally resolves references.
     *
     * @param logicalTableName The logical name of the table.
     * @param projectConfigId The UUID of the project.
     * @param systemRowId The system-generated UUID of the row.
     * @return An Optional containing the row data as a Map (with resolved references), or empty if not found.
     */
    @Transactional(readOnly = true)
    public Optional<Map<String, Object>> getSingleRowBySystemIdFromDynamicTable(String logicalTableName, String projectConfigId, String systemRowId) {
        JdbcTemplate jdbcTemplate = getJdbcTemplateForProject(projectConfigId);
        TableDefinition tableDef = getTableDefinitionByLogicalNameAndProject(logicalTableName, projectConfigId)
                .orElseThrow(() -> new IllegalArgumentException("Table definition not found for logical name: " + logicalTableName + " in project: " + projectConfigId));

        String finalTableName = tableDef.getFinalTableName();
        String sql = "SELECT * FROM `" + finalTableName + "` WHERE `" + SYSTEM_UUID_COLUMN_NAME + "` = ?"; // Quote table and column names

        try {
            Map<String, Object> row = jdbcTemplate.queryForMap(sql, systemRowId);
            return Optional.of(processReferencesInRow(row, tableDef, projectConfigId));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        } catch (Exception e) {
            throw new RuntimeException("Error fetching single row from dynamic table: " + e.getMessage(), e);
        }
    }

    /**
     * Updates data in a dynamic table based on a filter column and value.
     *
     * @param logicalTableName The logical name of the table.
     * @param projectConfigId The UUID of the project.
     * @param updateData A map of column names to new values.
     * @param filterColumn The column to use for filtering rows to update.
     * @param filterValue The value to match in the filterColumn.
     * @return The number of rows affected.
     */
    @Transactional
    public int updateDataInDynamicTable(String logicalTableName, String projectConfigId, Map<String, Object> updateData, String filterColumn, Object filterValue) {
        JdbcTemplate jdbcTemplate = getJdbcTemplateForProject(projectConfigId);
        TableDefinition tableDef = getTableDefinitionByLogicalNameAndProject(logicalTableName, projectConfigId)
                .orElseThrow(() -> new IllegalArgumentException("Table definition not found for logical name: " + logicalTableName + " in project: " + projectConfigId));

        for (ColumnDefinition colDef : tableDef.getColumns()) {
            if (colDef.isReference()) {
                String referencingColumnName = colDef.getColumnName();
                if (updateData.containsKey(referencingColumnName)) {
                    Object providedValue = updateData.get(referencingColumnName);
                    if (providedValue == null) {
                        if (!colDef.isNullable()) {
                            throw new IllegalArgumentException("Non-nullable reference column '" + referencingColumnName + "' cannot be null.");
                        }
                        continue;
                    }
                    if (!(providedValue instanceof String)) {
                        throw new IllegalArgumentException("Value for reference column '" + referencingColumnName + "' must be a String (UUID). Provided: " + providedValue.getClass().getSimpleName());
                    }
                    boolean exists = checkReferencedValueExists(
                            jdbcTemplate,
                            colDef.getReferencedTableIdRef(),
                            colDef.getReferencedColumnIdRef(),
                            (String) providedValue,
                            projectConfigId
                    );
                    if (!exists) {
                        throw new IllegalArgumentException(
                                "Invalid reference: Value '" + providedValue + "' for column '" + referencingColumnName +
                                        "' does not exist in the referenced table '" + colDef.getReferencedTableLogicalName() +
                                        "' (column: " + colDef.getReferencedColumnLogicalName() + ")."
                        );
                    }
                }
            }
        }

        return dynamicTableAccessor.update(jdbcTemplate, tableDef.getFinalTableName(), updateData, filterColumn, filterValue);
    }

    /**
     * Deletes data from a dynamic table based on a filter column and value.
     *
     * @param logicalTableName The logical name of the table.
     * @param projectConfigId The UUID of the project.
     * @param filterColumn The column to use for filtering rows to delete.
     * @param filterValue The value to match in the filterColumn.
     * @return The number of rows affected.
     */
    @Transactional
    public int deleteDataFromDynamicTable(String logicalTableName, String projectConfigId, String filterColumn, Object filterValue) {
        JdbcTemplate jdbcTemplate = getJdbcTemplateForProject(projectConfigId);
        TableDefinition tableDef = getTableDefinitionByLogicalNameAndProject(logicalTableName, projectConfigId)
                .orElseThrow(() -> new IllegalArgumentException("Table definition not found for logical name: " + logicalTableName + " in project: " + projectConfigId));

        return dynamicTableAccessor.delete(jdbcTemplate, tableDef.getFinalTableName(), filterColumn, filterValue);
    }

    /**
     * Retrieves all table definitions for a given project.
     *
     * @param projectConfigId The ID of the project configuration.
     * @return A list of TableDefinitionDto objects.
     */
    @Transactional(readOnly = true)
    public List<TableDefinitionDto> getAllTableDefinitionsForProject(String projectConfigId) {
        List<TableDefinition> tableDefs = tableDefinitionRepository.findByProjectConfigIdRef(projectConfigId);
        return tableDefs.stream()
                .map(TableDefinitionDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a specific TableDefinition along with a single row of data from it
     * identified by system_row_id.
     *
     * @param logicalTableName The logical name of the table.
     * @param projectConfigId The UUID of the project.
     * @param systemRowId The system-generated UUID of the row.
     * @return An Optional containing a Map with "tableDefinition" (as DTO) and "rowData" (as Map), or empty.
     */
    @Transactional(readOnly = true)
    public Optional<Map<String, Object>> getTableDefinitionAndSingleRow(String logicalTableName, String projectConfigId, String systemRowId) {
        Optional<TableDefinition> tableDefOpt = getTableDefinitionByLogicalNameAndProject(logicalTableName, projectConfigId);
        if (tableDefOpt.isEmpty()) {
            return Optional.empty();
        }

        TableDefinition tableDef = tableDefOpt.get();
        Optional<Map<String, Object>> rowDataOpt = getSingleRowBySystemIdFromDynamicTable(logicalTableName, projectConfigId, systemRowId);

        if (rowDataOpt.isEmpty()) {
            return Optional.empty();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("tableDefinition", TableDefinitionDto.fromEntity(tableDef));
        result.put("rowData", rowDataOpt.get());
        return Optional.of(result);
    }


    /**
     * Helper method to check if a value exists in a referenced table's column.
     *
     * @param jdbcTemplate The JdbcTemplate for the target database.
     * @param referencedTableId The UUID of the referenced TableDefinition.
     * @param referencedColumnId The UUID of the ColumnDefinition in the referenced table.
     * @param value The value to check for existence.
     * @param projectConfigId The UUID of the project.
     * @return True if the value exists, false otherwise.
     */
    private boolean checkReferencedValueExists(JdbcTemplate jdbcTemplate, String referencedTableId, String referencedColumnId, String value, String projectConfigId) {
        TableDefinition refTableDef = tableDefinitionRepository.findById(referencedTableId)
                .orElseThrow(() -> new IllegalStateException("Referenced table definition not found for ID: " + referencedTableId));

        ColumnDefinition refColumnDef = refTableDef.getColumns().stream()
                .filter(c -> c.getId().equals(referencedColumnId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Referenced column definition not found for ID: " + referencedColumnId + " in table " + refTableDef.getTableName()));

        return dynamicTableAccessor.checkRowExists(
                jdbcTemplate,
                refTableDef.getFinalTableName(),
                refColumnDef.getColumnName(),
                value
        );
    }


    /**
     * Helper method to resolve reference values in a given row.
     * It adds new entries to the map (e.g., column_name_display) with the lookup values.
     *
     * @param row The raw row data from the dynamic table.
     * @param tableDef The TableDefinition of the current table.
     * @param projectConfigId The UUID of the current project.
     * @return A new Map with resolved reference values.
     */
    private Map<String, Object> processReferencesInRow(Map<String, Object> row, TableDefinition tableDef, String projectConfigId) {
        Map<String, Object> processedRow = new HashMap<>(row);

        for (ColumnDefinition colDef : tableDef.getColumns()) {
            if (colDef.isReference() && colDef.getReferencedTableIdRef() != null && colDef.getReferencedColumnIdRef() != null) {
                String referencingColumnName = colDef.getColumnName();
                String referencedRowValue = (String) row.get(referencingColumnName);

                if (referencedRowValue != null && !referencedRowValue.isEmpty()) {
                    try {
                        TableDefinition referencedTableDef = tableDefinitionRepository.findById(colDef.getReferencedTableIdRef())
                                .orElse(null);

                        if (referencedTableDef != null) {
                            ColumnDefinition targetDisplayColumnDef = referencedTableDef.getColumns().stream()
                                    .filter(c -> c.getColumnName().equals(colDef.getReferencedColumnLogicalName()))
                                    .findFirst()
                                    .orElse(null);

                            if (targetDisplayColumnDef != null) {
                                JdbcTemplate refJdbcTemplate = getJdbcTemplateForProject(projectConfigId);

                                Optional<Map<String, Object>> refRowOpt = dynamicTableAccessor.selectById(
                                        refJdbcTemplate,
                                        referencedTableDef.getFinalTableName(),
                                        colDef.getReferencedColumnLogicalName(),
                                        referencedRowValue
                                );

                                refRowOpt.ifPresent(refRow -> {
                                    if (refRow.containsKey(colDef.getReferencedColumnLogicalName())) {
                                        processedRow.put(referencingColumnName + "_display", refRow.get(colDef.getReferencedColumnLogicalName()));
                                    }
                                    processedRow.put(referencingColumnName + "_details", refRow);
                                });
                            }
                        }
                    } catch (EmptyResultDataAccessException e) {
                        System.out.println("Referenced value " + referencedRowValue + " not found in " + colDef.getReferencedTableLogicalName());
                    } catch (Exception e) {
                        System.err.println("Error resolving reference for column " + referencingColumnName + " (refId: " + referencedRowValue + "): " + e.getMessage());
                    }
                }
            }
        }
        return processedRow;
    }

    // --- Utility Methods ---

    private JdbcTemplate getJdbcTemplateForProject(String projectConfigId) {
        ProjectConfig projectConfig = projectConfigService.getProjectConfigById(projectConfigId)
                .orElseThrow(() -> new IllegalArgumentException("Project configuration not found for ID: " + projectConfigId));
        DatabaseConnectionDetails dbDetails = databaseConnectivityService.getSavedConnectionByUuid(projectConfig.getDatabaseConnectionIdRef())
                .orElseThrow(() -> new IllegalStateException("Database connection details not found for project ID: " + projectConfigId));
        return dynamicDataSourceManager.getJdbcTemplate(dbDetails);
    }

    private String mapColumnTypeToSql(String columnType) {
        return switch (columnType.toLowerCase()) {
            case "varchar", "string" -> "VARCHAR(255)";
            case "text" -> "TEXT";
            case "int", "integer" -> "INT";
            case "long" -> "BIGINT";
            case "boolean" -> "BOOLEAN";
            case "date" -> "DATE";
            case "datetime" -> "DATETIME";
            case "decimal", "double" -> "DECIMAL(10, 2)";
            case "uuid" -> "VARCHAR(36)";
            case "reference" -> "VARCHAR(36)";
            case "email" -> "VARCHAR(36)";
            case "password" -> "VARCHAR(36)";
            default -> throw new IllegalArgumentException("Unsupported column type: " + columnType);
        };
    }


    /**
     * Retrieves a specific TableDefinition by its ID and ensures it belongs to the given project.
     *
     * @param tableDefinitionId The UUID of the TableDefinition.
     * @param projectConfigId The UUID of the project to which the table must belong.
     * @return An Optional containing the TableDefinitionDto if found and belongs to the project, otherwise empty.
     */
    @Transactional(readOnly = true)
    public Optional<TableDefinitionDto> getTableDefinitionByIdAndProject(String tableDefinitionId, String projectConfigId) {
        return tableDefinitionRepository.findById(tableDefinitionId)
                .filter(td -> td.getProjectConfigIdRef().equals(projectConfigId))
                .map(TableDefinitionDto::fromEntity);
    }

    /**
     * Helper method to get TableDefinition by logical name and project ID.
     * Use this if you need to fetch by logical name, not by UUID.
     *
     * @param logicalTableName The logical name of the table.
     * @param projectConfigId The UUID of the project.
     * @return Optional of TableDefinition.
     */
    @Transactional(readOnly = true)
    public Optional<TableDefinition> getTableDefinitionByLogicalNameAndProject(String logicalTableName, String projectConfigId) {
        return tableDefinitionRepository.findByTableNameAndProjectConfigIdRef(logicalTableName, projectConfigId);
    }

    /**
     * Retrieves all data rows from a dynamic table along with its column definitions.
     *
     * @param logicalTableName The logical name of the table.
     * @param projectConfigId The UUID of the project configuration.
     * @return A TableDataResponseDto containing column definitions and all rows of data.
     * @throws IllegalArgumentException if the table definition is not found for the given project.
     */
    @Transactional(readOnly = true)
    public TableDataResponseDto getCombinedTableData(String logicalTableName, String projectConfigId) {
        // 1. Get the TableDefinition (which includes ColumnDefinitions)
        TableDefinition tableDef = getTableDefinitionByLogicalNameAndProject(logicalTableName, projectConfigId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Table definition '" + logicalTableName + "' not found for project '" + projectConfigId + "'."));

        // 2. Convert ColumnDefinition entities to DTOs
        List<ColumnDefinitionDto> columnDefsDto = tableDef.getColumns().stream()
                .map(ColumnDefinitionDto::fromEntity)
                .collect(Collectors.toList());

        // 3. Get all data from the dynamic table
        List<Map<String, Object>> rowData = getAllDataFromDynamicTable(logicalTableName, projectConfigId);

        // 4. Build the combined response DTO
        return TableDataResponseDto.builder()
                .columnDefinitions(columnDefsDto)
                .rowData(rowData)
                .build();
    }

    /**
     * Fetches all data from a dynamically created table.
     * This method is called internally by getCombinedTableData.
     *
     * @param logicalTableName The logical name of the table.
     * @param projectConfigId The ID of the project configuration.
     * @return A list of maps, where each map represents a row.
     * @throws IllegalArgumentException if the table definition is not found or database connection fails.
     */
    // This method already exists, just ensuring its visibility for context.
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllDataFromDynamicTable(String logicalTableName, String projectConfigId) {
        ProjectConfig projectConfig = projectConfigService.getProjectConfigById(projectConfigId)
                .orElseThrow(() -> new IllegalArgumentException("Project configuration not found with ID: " + projectConfigId));

        TableDefinition tableDefinition = tableDefinitionRepository.findByTableNameAndProjectConfigIdRef(logicalTableName, projectConfigId)
                .orElseThrow(() -> new IllegalArgumentException("Table definition '" + logicalTableName + "' not found for project '" + projectConfigId + "'."));

        DatabaseConnectionDetails dbDetails = databaseConnectivityService.getDatabaseConnectionDetails(projectConfig.getDatabaseConnectionIdRef())
                .orElseThrow(() -> new IllegalArgumentException("Database connection details not found for ID: " + projectConfig.getDatabaseConnectionIdRef()));

        try {
            JdbcTemplate jdbcTemplate = dynamicDataSourceManager.getJdbcTemplate(dbDetails);
            // Assuming getFinalTableName() works
            String sql = "SELECT * FROM " + tableDefinition.getFinalTableName();
            List<Map<String, Object>> rawRows = jdbcTemplate.queryForList(sql);

            // Resolve references if any
            return dynamicTableAccessor.resolveReferenceColumns(jdbcTemplate,tableDefinition, rawRows);

        } catch (Exception e) {
            throw new RuntimeException("Error fetching all data from table '" + logicalTableName + "': " + e.getMessage(), e);
        }
    }
}