package com.easy.auth.dynamicdata.service;




import com.easy.tabledef.model.TableDefinition;
import com.easy.tabledef.service.TableCreationService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DynamicDataService {

    private final JdbcTemplate jdbcTemplate;
    private final TableCreationService tableDefinitionService; // To get table/column info

    public DynamicDataService(JdbcTemplate jdbcTemplate, TableCreationService tableDefinitionService) {
        this.jdbcTemplate = jdbcTemplate;
        this.tableDefinitionService = tableDefinitionService;
    }

    // --- CRUD for dynamic tables ---

    // Example: Create a new row in a dynamic table
    public Map<String, Object> createRow(String tableName, Map<String, Object> rowData) {
        TableDefinition tableMetadata = tableDefinitionService.getTableDefinitionByLogicalName(tableName)
                .orElseThrow(() -> new IllegalArgumentException("Table not found: " + tableName));

        // Prepare SQL INSERT statement
        String columns = rowData.keySet().stream().collect(Collectors.joining(", "));
        String placeholders = rowData.keySet().stream().map(k -> "?").collect(Collectors.joining(", "));
        String sql = String.format("INSERT INTO %s (%s) VALUES (%s)", tableName, columns, placeholders);

        Object[] values = rowData.values().toArray();
        jdbcTemplate.update(sql, values);

        // In a real app, you'd want to return the newly created row with its ID
        // This often requires a "RETURNING id" clause or a separate SELECT.
        return rowData; // Placeholder return
    }

    // Example: Read all rows from a dynamic table
    public List<Map<String, Object>> getAllRows(String tableName) {
        // Basic validation for table name to prevent SQL injection
        if (!tableName.matches("^[a-zA-Z0-9_]+$")) {
            throw new IllegalArgumentException("Invalid table name.");
        }
        String sql = "SELECT * FROM " + tableName;
        return jdbcTemplate.queryForList(sql);
    }

    // Example: Read a single row by ID from a dynamic table
    public Map<String, Object> getRowById(String tableName, Long id) {
        // Need to know the primary key column name. Assume 'id' for simplicity.
        // In a real system, you'd retrieve this from `ColumnMetadata`.
        if (!tableName.matches("^[a-zA-Z0-9_]+$")) {
            throw new IllegalArgumentException("Invalid table name.");
        }
        String sql = String.format("SELECT * FROM %s WHERE id = ?", tableName);
        return jdbcTemplate.queryForMap(sql, id);
    }

    // Example: Update a row in a dynamic table
    public void updateRow(String tableName, Long id, Map<String, Object> rowData) {
        // Prepare SQL UPDATE statement
        String setClause = rowData.keySet().stream()
                .map(key -> key + " = ?")
                .collect(Collectors.joining(", "));
        String sql = String.format("UPDATE %s SET %s WHERE id = ?", tableName, setClause);

        Object[] values = new Object[rowData.size() + 1];
        int i = 0;
        for (Object value : rowData.values()) {
            values[i++] = value;
        }
        values[i] = id; // Last value is the ID for the WHERE clause

        jdbcTemplate.update(sql, values);
    }

    // Example: Delete a row from a dynamic table
    public void deleteRow(String tableName, Long id) {
        if (!tableName.matches("^[a-zA-Z0-9_]+$")) {
            throw new IllegalArgumentException("Invalid table name.");
        }
        String sql = String.format("DELETE FROM %s WHERE id = ?", tableName);
        jdbcTemplate.update(sql, id);
    }
}