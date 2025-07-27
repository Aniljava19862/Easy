/*package com.easy.auth.dynamicdata.service;

import com.easy.tabledef.service.TableCreationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional; // Required for Optional return types

@Service
public class DynamicDataService {

    // You should *not* directly inject JdbcTemplate here for dynamic operations.
    // Instead, rely on TableCreationService to provide the correct dynamic JdbcTemplate.
    // private final JdbcTemplate jdbcTemplate; // REMOVE THIS LINE

    private final TableCreationService tableCreationService; // Corrected variable name

    @Autowired // Use @Autowired for constructor injection
    public DynamicDataService(TableCreationService tableCreationService) {
        // No direct JdbcTemplate here
        this.tableCreationService = tableCreationService;
    }

    // --- CRUD for dynamic tables, delegating to TableCreationService ---

    /**
     * Creates a new row in a dynamic table for a specific project.
     * Delegates to TableCreationService.
     *
     * @param logicalTableName The logical name of the table.
     * @param projectConfigId The UUID of the project.
     * @param rowData A map of column names to their values for the new row.
     * @return The map of rowData, including the generated system_row_id.
     */
   /* public Map<String, Object> createRow(String logicalTableName, String projectConfigId, Map<String, Object> rowData) {
        // TableCreationService's addDataToDynamicTable already handles UUID generation
        // and uses the correct dynamic JdbcTemplate via getJdbcTemplateForProject.
        // It returns an int (rows affected), but we can adapt it to return the map.
        // As addDataToDynamicTable modifies the input 'rowData' to add SYSTEM_UUID_COLUMN_NAME,
        // we can simply return the modified rowData after the call.
        tableCreationService.addDataToDynamicTable(logicalTableName, projectConfigId, rowData);
        return rowData; // rowData now contains the system_row_id
    }

    /**
     * Retrieves all rows from a dynamic table for a specific project.
     * Delegates to TableCreationService.
     *
     * @param logicalTableName The logical name of the table.
     * @param projectConfigId The UUID of the project.
     * @return A list of maps, where each map represents a row.
     */
   /* public List<Map<String, Object>> getAllRows(String logicalTableName, String projectConfigId) {
        return tableCreationService.getAllDataFromDynamicTable(logicalTableName, projectConfigId);
    }

    /**
     * Retrieves a single row by its system_row_id from a dynamic table for a specific project.
     * Delegates to TableCreationService.
     *
     * @param logicalTableName The logical name of the table.
     * @param projectConfigId The UUID of the project.
     * @param systemRowId The system-generated UUID of the row.
     * @return An Optional containing the row data as a Map, or empty if not found.
     */
/**  public Optional<Map<String, Object>> getRowBySystemId(String logicalTableName, String projectConfigId, String systemRowId) {
        // Renamed from getRowById to getRowBySystemId to match our architecture's primary key
        return tableCreationService.getSingleRowBySystemIdFromDynamicTable(logicalTableName, projectConfigId, systemRowId);
    }

    /**
     * Retrieves a single row by a custom filter column and value from a dynamic table for a specific project.
     * Delegates to TableCreationService.
     *
     * @param logicalTableName The logical name of the table.
     * @param projectConfigId The UUID of the project.
     * @param filterColumn The column name to filter by.
     * @param filterValue The value to match.
     * @return An Optional containing the row data as a Map, or empty if not found.
     */
/** public Optional<Map<String, Object>> getRowByFilter(String logicalTableName, String projectConfigId, String filterColumn, Object filterValue) {
        List<Map<String, Object>> results = tableCreationService.getFilteredDataFromDynamicTable(logicalTableName, projectConfigId, filterColumn, filterValue);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0)); // Return first match
    }

    /**
     * Updates a row in a dynamic table for a specific project.
     * Delegates to TableCreationService.
     *
     * @param logicalTableName The logical name of the table.
     * @param projectConfigId The UUID of the project.
     * @param updateData A map of column names to new values.
     * @param filterColumn The column to filter by for the update.
     * @param filterValue The value to match for the update.
     * @return The number of rows affected.
     */
/** public int updateRow(String logicalTableName, String projectConfigId, Map<String, Object> updateData, String filterColumn, Object filterValue) {
        return tableCreationService.updateDataInDynamicTable(logicalTableName, projectConfigId, updateData, filterColumn, filterValue);
    }

    /**
     * Deletes a row from a dynamic table for a specific project.
     * Delegates to TableCreationService.
     *
     * @param logicalTableName The logical name of the table.
     * @param projectConfigId The UUID of the project.
     * @param filterColumn The column to filter by for deletion.
     * @param filterValue The value to match for deletion.
     * @return The number of rows affected.
     */
/**  public int deleteRow(String logicalTableName, String projectConfigId, String filterColumn, Object filterValue) {
        return tableCreationService.deleteDataFromDynamicTable(logicalTableName, projectConfigId, filterColumn, filterValue);
    }

    // You can add more methods here if you need to expose other TableCreationService functionalities
    // or combine them in specific ways for your application's logic.
}*/