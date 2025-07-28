package com.easy.tabledef.util;

import com.easy.tabledef.model.ColumnDefinition;
import com.easy.tabledef.model.TableDefinition;
import com.easy.tabledef.repository.TableDefinitionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class DynamicTableAccessor {

    // --- CRUD operations for dynamic tables ---
    @Autowired
    private TableDefinitionRepository tableDefinitionRepository;
    public int insert(JdbcTemplate jdbcTemplate, String tableName, Map<String, Object> rowData) {
        // Basic validation for table name to prevent SQL injection
        if (!tableName.matches("^[a-zA-Z0-9_]+$")) {
            throw new IllegalArgumentException("Invalid table name: " + tableName);
        }

        Map<String, Object> filteredRowData = rowData.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        String columns = filteredRowData.keySet().stream()
                .map(key -> "`" + key + "`") // Quote column names
                .collect(Collectors.joining(", "));
        String placeholders = filteredRowData.keySet().stream()
                .map(key -> "?")
                .collect(Collectors.joining(", "));

        String sql = String.format("INSERT INTO `%s` (%s) VALUES (%s)", tableName, columns, placeholders);
        Object[] values = filteredRowData.values().toArray();

        return jdbcTemplate.update(sql, values);
    }

    public List<Map<String, Object>> selectAll(JdbcTemplate jdbcTemplate, String tableName) {
        if (!tableName.matches("^[a-zA-Z0-9_]+$")) {
            throw new IllegalArgumentException("Invalid table name: " + tableName);
        }
        String sql = "SELECT * FROM `" + tableName + "`";
        return jdbcTemplate.queryForList(sql);
    }

    /**
     * Selects a single row by a specified column and its value.
     *
     * @param jdbcTemplate The JdbcTemplate for the target database.
     * @param tableName The physical name of the table.
     * @param idColumnName The name of the column to filter by (e.g., "system_row_id", or a unique business ID).
     * @param idValue The value to match in the idColumnName.
     * @return An Optional containing the row data, or empty if not found.
     */
    public Optional<Map<String, Object>> selectById(JdbcTemplate jdbcTemplate, String tableName, String idColumnName, Object idValue) {
        if (!tableName.matches("^[a-zA-Z0-9_]+$") || !idColumnName.matches("^[a-zA-Z0-9_]+$")) {
            throw new IllegalArgumentException("Invalid table or column name for selectById.");
        }
        String sql = String.format("SELECT * FROM `%s` WHERE `%s` = ?", tableName, idColumnName);
        try {
            return Optional.of(jdbcTemplate.queryForMap(sql, idValue));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        } catch (Exception e) {
            throw new RuntimeException("Error selecting by ID from dynamic table '" + tableName + "' (column: " + idColumnName + ", value: " + idValue + "): " + e.getMessage(), e);
        }
    }

    public int update(JdbcTemplate jdbcTemplate, String tableName, Map<String, Object> updateData, String filterColumn, Object filterValue) {
        if (!tableName.matches("^[a-zA-Z0-9_]+$") || !filterColumn.matches("^[a-zA-Z0-9_]+$")) {
            throw new IllegalArgumentException("Invalid table or filter column name for update.");
        }

        String setClause = updateData.keySet().stream()
                .map(key -> "`" + key + "` = ?") // Quote column names
                .collect(Collectors.joining(", "));
        String sql = String.format("UPDATE `%s` SET %s WHERE `%s` = ?", tableName, setClause, filterColumn);

        Object[] values = new Object[updateData.size() + 1];
        int i = 0;
        for (Object value : updateData.values()) {
            values[i++] = value;
        }
        values[i] = filterValue;

        return jdbcTemplate.update(sql, values);
    }

    public int delete(JdbcTemplate jdbcTemplate, String tableName, String filterColumn, Object filterValue) {
        if (!tableName.matches("^[a-zA-Z0-9_]+$") || !filterColumn.matches("^[a-zA-Z0-9_]+$")) {
            throw new IllegalArgumentException("Invalid table or filter column name for delete.");
        }
        String sql = String.format("DELETE FROM `%s` WHERE `%s` = ?", tableName, filterColumn);
        return jdbcTemplate.update(sql, filterValue);
    }

    /**
     * Checks if a row with a specific value exists in a given column of a table.
     *
     * @param jdbcTemplate The JdbcTemplate for the target database.
     * @param finalTableName The physical name of the table.
     * @param columnName The name of the column to check.
     * @param value The value to look for.
     * @return True if a row exists, false otherwise.
     */
    public boolean checkRowExists(JdbcTemplate jdbcTemplate, String finalTableName, String columnName, String value) {
        if (!finalTableName.matches("^[a-zA-Z0-9_]+$") || !columnName.matches("^[a-zA-Z0-9_]+$")) {
            throw new IllegalArgumentException("Invalid table or column name for existence check.");
        }
        String sql = String.format("SELECT COUNT(*) FROM `%s` WHERE `%s` = ?", finalTableName, columnName);
        try {
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, value);
            return count != null && count > 0;
        } catch (Exception e) {
            System.err.println("Error checking row existence in table " + finalTableName + " for column " + columnName + " with value " + value + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Resolves reference columns in a list of raw data rows using the provided JdbcTemplate.
     * It replaces the reference UUID with a display name and adds a separate _id field.
     * THIS IS THE MISSING METHOD.
     *
     * @param jdbcTemplate The JdbcTemplate connected to the correct dynamic database (passed from service).
     * @param currentTableDef The TableDefinition of the table from which rows were fetched.
     * @param rawRows A list of maps, where each map is a raw row from the database.
     * @return A list of maps with reference columns resolved.
     */
    public List<Map<String, Object>> resolveReferenceColumns(
            JdbcTemplate jdbcTemplate, // This parameter is crucial
            TableDefinition currentTableDef,
            List<Map<String, Object>> rawRows) {

        if (rawRows == null || rawRows.isEmpty()) {
            return rawRows;
        }

        if (jdbcTemplate == null) {
            // This indicates a configuration error where JdbcTemplate wasn't provided.
            System.err.println("JdbcTemplate is null in DynamicTableAccessor.resolveReferenceColumns. Reference resolution will fail for some rows.");
            // Return raw rows as a fallback
            return rawRows;
        }

        // Identify which columns in the current table are references
        List<ColumnDefinition> referenceColumns = currentTableDef.getColumns().stream()
                .filter(ColumnDefinition::isReference)
                .collect(Collectors.toList());

        if (referenceColumns.isEmpty()) {
            return rawRows; // No reference columns to resolve, return as is
        }

        List<Map<String, Object>> resolvedRows = new ArrayList<>();

        for (Map<String, Object> rawRow : rawRows) {
            Map<String, Object> newRow = new HashMap<>(rawRow); // Create a mutable copy of the row

            for (ColumnDefinition refCol : referenceColumns) {
                String refColumnName = refCol.getColumnName(); // e.g., "manager_ref"
                Object refId = rawRow.get(refColumnName); // Get the UUID value from the current row

                // Only attempt to resolve if the reference ID is not null or empty
                if (refId != null && !refId.toString().isEmpty()) {
                    String referencedTableId = refCol.getReferencedTableIdRef(); // UUID of the referenced TableDefinition
                    String referencedColumnLogicalName = refCol.getReferencedColumnLogicalName(); // e.g., "system_row_id" in the referenced table

                    // Step 1: Look up the definition of the referenced table using its ID
                    Optional<TableDefinition> referencedTableOpt = tableDefinitionRepository.findById(referencedTableId);

                    if (referencedTableOpt.isPresent()) {
                        TableDefinition referencedTableDef = referencedTableOpt.get();
                        String referencedFinalTableName = referencedTableDef.getFinalTableName(); // Actual database table name

                        // Step 2: Determine the display column from the referenced table's definition
                        // Prioritize "name", "display_name", then any VARCHAR/TEXT column, fallback to the referenced ID column itself.
                        String displayColumnToFetch = referencedTableDef.getColumns().stream()
                                .filter(c -> c.getColumnName().equalsIgnoreCase("name") ||
                                        c.getColumnName().equalsIgnoreCase("display_name") ||
                                        c.getColumnType().equalsIgnoreCase("varchar") ||
                                        c.getColumnType().equalsIgnoreCase("text"))
                                .map(ColumnDefinition::getColumnName)
                                .findFirst()
                                .orElse(referencedColumnLogicalName); // Fallback: use the column that holds the ID

                        // Step 3: Construct and execute the SQL query to get the display value
                        String querySql = String.format("SELECT %s FROM %s WHERE %s = ?",
                                displayColumnToFetch, referencedFinalTableName, referencedColumnLogicalName);

                        try {
                            // QueryForObject expects exactly one result; if not found, EmptyResultDataAccessException is thrown.
                            String displayValue = jdbcTemplate.queryForObject(querySql, String.class, refId.toString());
                            newRow.put(refColumnName + "_id", refId); // Add the ID with _id suffix
                            newRow.put(refColumnName + "_display_name", displayValue); // Add the resolved display name
                            newRow.remove(refColumnName); // Remove the original column (which contained just the ID)
                        } catch (EmptyResultDataAccessException e) {
                            // No matching row found in the referenced table for this ID
                            newRow.put(refColumnName + "_id", refId);
                            newRow.put(refColumnName + "_display_name", null); // Or "[Not Found]"
                            newRow.remove(refColumnName);
                        } catch (Exception e) {
                            // General SQL or other exceptions during lookup
                            System.err.println("Error resolving reference column '" + refColumnName + "' for ID '" + refId + "': " + e.getMessage());
                            newRow.put(refColumnName + "_id", refId);
                            newRow.put(refColumnName + "_display_name", "[Error]"); // Indicate an error occurred
                            newRow.remove(refColumnName);
                        }
                    } else {
                        // The TableDefinition for the referenced table was not found in our metadata
                        newRow.put(refColumnName + "_id", refId);
                        newRow.put(refColumnName + "_display_name", "[Ref Table Def Missing]");
                        newRow.remove(refColumnName);
                    }
                } else {
                    // The original reference ID was null or empty in the row
                    newRow.put(refColumnName + "_id", null);
                    newRow.put(refColumnName + "_display_name", null);
                    newRow.remove(refColumnName);
                }
            }
            resolvedRows.add(newRow); // Add the potentially modified row to the list
        }

        return resolvedRows;
    }
}