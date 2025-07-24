package com.easy.tabledef.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component; // <--- This annotation makes it a Spring component
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A Spring-managed component for performing CRUD operations on dynamically created
 * database tables using JdbcTemplate. This single instance can operate on any dynamic table
 * by passing the table name as a method parameter.
 */
@Component
public class DynamicTableAccessor {

    private final JdbcTemplate jdbcTemplate;

    // Pattern to validate SQL identifiers to prevent SQL injection for table/column names.
    private static final Pattern SQL_IDENTIFIER_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");

    /**
     * Spring automatically injects the JdbcTemplate instance into this constructor.
     *
     * @param jdbcTemplate The JdbcTemplate instance provided by Spring.
     */
    @Autowired
    public DynamicTableAccessor(JdbcTemplate jdbcTemplate) {
        Assert.notNull(jdbcTemplate, "JdbcTemplate cannot be null.");
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Validates and sanitizes a string to be a safe SQL identifier (table or column name).
     * Throws IllegalArgumentException if the name is invalid.
     *
     * @param name The identifier to sanitize.
     * @return The sanitized identifier.
     */
    private String sanitizeSqlIdentifier(String name) {
        Assert.hasText(name, "SQL identifier cannot be null or empty.");
        if (!SQL_IDENTIFIER_PATTERN.matcher(name).matches()) {
            throw new IllegalArgumentException("Invalid SQL identifier: '" + name + "'. Must be alphanumeric and start with a letter or underscore.");
        }
        return name;
    }

    //--------------------------------------------------------------------------------
    // Create Operation (Insert)
    //--------------------------------------------------------------------------------

    /**
     * Inserts a single row of data into a dynamically created table.
     *
     * @param tableName The actual physical name of the table (e.g., "users_data", "transactions_2025_q2").
     * @param data      A Map where keys are column names (String) and values are the data (Object).
     * Example: Map.of("id", 1, "name", "Alice", "amount", 100.50)
     * @return The number of rows affected (should be 1 for a successful insert).
     * @throws IllegalArgumentException if the table name or data is invalid.
     */
    /**
     * Inserts data into a specified table.
     * This method expects the 'data' map to already contain the 'system_row_id' if applicable.
     *
     * @param tableName The physical name of the table.
     * @param data      A Map<String, Object> where keys are column names and values are the data.
     * @return The number of rows affected.
     */
    public int insert(String tableName, Map<String, Object> data) {
        String sanitizedTableName = sanitizeSqlIdentifier(tableName);
        Assert.notEmpty(data, "Data for insertion cannot be empty.");

        // Separate column names and their corresponding values
        List<String> columns = data.keySet().stream()
                .map(this::sanitizeSqlIdentifier) // Sanitize column names
                .collect(Collectors.toList());

        // Create a list of values, ensuring their order matches the columns list.
        // It's important to get values based on the order of the *original* unsanitized map keys
        // or by mapping from the sanitized keys back to the original for lookup if your sanitize adds chars.
        // The .replace("`", "") handles cases where sanitizeSqlIdentifier might add backticks.
        List<Object> values = columns.stream()
                .map(sanitizedColName -> data.get(sanitizedColName.replaceAll("`", "")))
                .collect(Collectors.toList());


        // Create placeholders for prepared statement (e.g., ?, ?, ?)
        String placeholders = String.join(", ", Collections.nCopies(columns.size(), "?"));

        // Build the INSERT SQL statement
        String sql = String.format("INSERT INTO %s (%s) VALUES (%s)",
                sanitizedTableName,
                String.join(", ", columns),
                placeholders);

        System.out.println("Executing INSERT on " + sanitizedTableName + ": " + sql + " with values: " + values);
        return jdbcTemplate.update(sql, values.toArray());
    }

    //--------------------------------------------------------------------------------
    // Read Operations (List)
    //--------------------------------------------------------------------------------

    /**
     * Reads all rows from a dynamically created table.
     *
     * @param tableName The actual physical name of the table.
     * @return A List of Maps, where each Map represents a row (columnName -> value).
     * Returns an empty list if the table is empty or not found.
     * @throws IllegalArgumentException if the table name is invalid.
     */
    public List<Map<String, Object>> readAll(String tableName) {
        String sanitizedTableName = sanitizeSqlIdentifier(tableName);
        String sql = String.format("SELECT * FROM %s", sanitizedTableName);

        System.out.println("Executing READ ALL on " + sanitizedTableName + ": " + sql);
        return jdbcTemplate.queryForList(sql);
    }

    /**
     * Reads rows from a dynamically created table based on a single filter condition.
     *
     * @param tableName    The actual physical name of the table.
     * @param filterColumn The column name to filter by.
     * @param filterValue  The value to match in the filterColumn.
     * @return A List of Maps, where each Map represents a row.
     * @throws IllegalArgumentException if table name, filter column, or filter value is invalid.
     */
    public List<Map<String, Object>> readByFilter(String tableName, String filterColumn, Object filterValue) {
        String sanitizedTableName = sanitizeSqlIdentifier(tableName);
        String sanitizedFilterColumn = sanitizeSqlIdentifier(filterColumn);
        Assert.notNull(filterValue, "Filter value cannot be null.");

        String sql = String.format("SELECT * FROM %s WHERE %s = ?", sanitizedTableName, sanitizedFilterColumn);

        System.out.println("Executing READ BY FILTER on " + sanitizedTableName + ": " + sql + " with value: " + filterValue);
        return jdbcTemplate.queryForList(sql, filterValue);
    }

    /**
     * Reads rows from a dynamically created table based on multiple filter conditions (ANDed together).
     *
     * @param tableName The actual physical name of the table.
     * @param filters   A Map where keys are column names (String) and values are the filter criteria (Object).
     * Example: Map.of("status", "completed", "user_id", 123)
     * @return A List of Maps, where each Map represents a row.
     * @throws IllegalArgumentException if table name or filters are invalid.
     */
    public List<Map<String, Object>> readByMultipleFilters(String tableName, Map<String, Object> filters) {
        String sanitizedTableName = sanitizeSqlIdentifier(tableName);
        Assert.notEmpty(filters, "Filters cannot be empty.");

        StringBuilder whereClause = new StringBuilder();
        List<Object> args = new java.util.ArrayList<>();

        // Build the WHERE clause (e.g., "WHERE column1 = ? AND column2 = ?")
        whereClause.append(" WHERE ");
        String conditions = filters.keySet().stream()
                .map(col -> {
                    args.add(filters.get(col)); // Add value to arguments list
                    return sanitizeSqlIdentifier(col) + " = ?";
                })
                .collect(Collectors.joining(" AND "));
        whereClause.append(conditions);

        String sql = String.format("SELECT * FROM %s%s", sanitizedTableName, whereClause);

        System.out.println("Executing READ BY MULTIPLE FILTERS on " + sanitizedTableName + ": " + sql + " with values: " + args);
        return jdbcTemplate.queryForList(sql, args.toArray());
    }

    //--------------------------------------------------------------------------------
    // Update Operation
    //--------------------------------------------------------------------------------

    /**
     * Updates rows in a dynamically created table based on a single filter condition.
     *
     * @param tableName    The actual physical name of the table.
     * @param updateData   A Map where keys are column names to update (String) and values are new data (Object).
     * Example: Map.of("status", "processed", "updated_at", LocalDateTime.now())
     * @param filterColumn The column name for the WHERE clause.
     * @param filterValue  The value to match in the filterColumn.
     * @return The number of rows affected by the update.
     * @throws IllegalArgumentException if table name, update data, or filter condition is invalid.
     */
    public int update(String tableName, Map<String, Object> updateData, String filterColumn, Object filterValue) {
        String sanitizedTableName = sanitizeSqlIdentifier(tableName);
        String sanitizedFilterColumn = sanitizeSqlIdentifier(filterColumn);
        Assert.notEmpty(updateData, "Update data cannot be empty.");
        Assert.notNull(filterValue, "Filter value cannot be null.");

        List<Object> args = new java.util.ArrayList<>();
        String setClause = updateData.keySet().stream()
                .map(col -> {
                    args.add(updateData.get(col)); // Add update value to arguments list
                    return sanitizeSqlIdentifier(col) + " = ?";
                })
                .collect(Collectors.joining(", "));

        // Add the filter value to the end of arguments for the WHERE clause
        args.add(filterValue);

        String sql = String.format("UPDATE %s SET %s WHERE %s = ?", sanitizedTableName, setClause, sanitizedFilterColumn);

        System.out.println("Executing UPDATE on " + sanitizedTableName + ": " + sql + " with values: " + args);
        return jdbcTemplate.update(sql, args.toArray());
    }

    //--------------------------------------------------------------------------------
    // Delete Operation
    //--------------------------------------------------------------------------------

    /**
     * Deletes rows from a dynamically created table based on a single filter condition.
     *
     * @param tableName    The actual physical name of the table.
     * @param filterColumn The column name for the WHERE clause.
     * @param filterValue  The value to match in the filterColumn.
     * @return The number of rows affected by the delete.
     * @throws IllegalArgumentException if table name, filter column, or filter value is invalid.
     */
    public int delete(String tableName, String filterColumn, Object filterValue) {
        String sanitizedTableName = sanitizeSqlIdentifier(tableName);
        String sanitizedFilterColumn = sanitizeSqlIdentifier(filterColumn);
        Assert.notNull(filterValue, "Filter value cannot be null.");

        String sql = String.format("DELETE FROM %s WHERE %s = ?", sanitizedTableName, sanitizedFilterColumn);

        System.out.println("Executing DELETE on " + sanitizedTableName + ": " + sql + " with value: " + filterValue);
        return jdbcTemplate.update(sql, filterValue);
    }

    // In src/main/java/com/easy/tabledef/util/DynamicTableAccessor.java
// ... inside DynamicTableAccessor class ...

    public Optional<Map<String, Object>> selectOne(String tableName, String keyColumn, Object keyValue) {
        String sanitizedTableName = sanitizeSqlIdentifier(tableName);
        String sanitizedKeyColumn = sanitizeSqlIdentifier(keyColumn);

        String sql = String.format("SELECT * FROM %s WHERE %s = ?", sanitizedTableName, sanitizedKeyColumn);

        try {
            // queryForList is safer than queryForMap for single results, as it returns an empty list if no match
            List<Map<String, Object>> result = jdbcTemplate.queryForList(sql, keyValue);
            return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
        } catch (org.springframework.dao.DataAccessException e) {
            // Catch more general DataAccessException for issues like column not found, etc.
            System.err.println("Error selecting one from " + tableName + " by " + keyColumn + "=" + keyValue + ": " + e.getMessage());
            throw new RuntimeException("Database error during select: " + e.getMessage(), e);
        }
    }
}