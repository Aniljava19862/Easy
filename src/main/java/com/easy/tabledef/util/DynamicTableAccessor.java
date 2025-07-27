package com.easy.tabledef.util;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class DynamicTableAccessor {

    // --- CRUD operations for dynamic tables ---

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
}