package com.easy.auth.dynamicdata.controller;

import com.easy.tabledef.service.TableCreationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap; // Added import for HashMap
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/projects/{projectConfigId}/dynamic-data")
public class DynamicDataController {

    @Autowired
    private TableCreationService tableCreationService;

    /**
     * Adds a new row of data to a dynamic table.
     *
     * @param projectConfigId The UUID of the project.
     * @param logicalTableName The logical name of the table to add data to.
     * @param data A map representing the row data (columnName -> value).
     * @return ResponseEntity with success message or error.
     */
    @PostMapping("/{logicalTableName}")
    public ResponseEntity<?> addDynamicData(
            @PathVariable String projectConfigId,
            @PathVariable String logicalTableName,
            @RequestBody Map<String, Object> data) {
        try {
            int rowsAffected = tableCreationService.addDataToDynamicTable(logicalTableName, projectConfigId, data);
            // Consistent response for success
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("message", "Data added successfully to table '" + logicalTableName + "'.");
            responseBody.put("rowsAffected", rowsAffected);
            return ResponseEntity.status(HttpStatus.CREATED).body(responseBody);
        } catch (IllegalArgumentException e) {
            // Consistent error response
            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("message", e.getMessage());
            errorBody.put("status", HttpStatus.BAD_REQUEST.value());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody);
        } catch (RuntimeException e) {
            // Consistent error response
            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("message", "Failed to add data: " + e.getMessage());
            errorBody.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody);
        }
    }

    /**
     * Retrieves all data from a specific dynamic table.
     *
     * @param projectConfigId The UUID of the project.
     * @param logicalTableName The logical name of the table to retrieve data from.
     * @return ResponseEntity with a list of maps, each representing a row.
     */
    @GetMapping("/{logicalTableName}")
    public ResponseEntity<?> getAllDynamicData(
            @PathVariable String projectConfigId,
            @PathVariable String logicalTableName) {
        try {
            List<Map<String, Object>> data = tableCreationService.getAllDataFromDynamicTable(logicalTableName, projectConfigId);
            if (data.isEmpty()) {
                // Consistent "not found" response
                Map<String, Object> errorBody = new HashMap<>();
                errorBody.put("message", "No data found for table '" + logicalTableName + "'.");
                errorBody.put("status", HttpStatus.NOT_FOUND.value());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody);
            }
            return ResponseEntity.ok(data);
        } catch (IllegalArgumentException e) {
            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("message", e.getMessage());
            errorBody.put("status", HttpStatus.NOT_FOUND.value());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody);
        } catch (RuntimeException e) {
            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("message", "Failed to retrieve data: " + e.getMessage());
            errorBody.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody);
        }
    }

    /**
     * Retrieves a single row from a dynamic table by its system_row_id.
     *
     * @param projectConfigId The UUID of the project.
     * @param logicalTableName The logical name of the table.
     * @param systemRowId The system-generated UUID of the row.
     * @return ResponseEntity with the row data or not found.
     */
    @GetMapping("/{logicalTableName}/{systemRowId}")
    public ResponseEntity<?> getDynamicDataBySystemId(
            @PathVariable String projectConfigId,
            @PathVariable String logicalTableName,
            @PathVariable String systemRowId) {
        try {
            Optional<Map<String, Object>> data = tableCreationService.getSingleRowBySystemIdFromDynamicTable(logicalTableName, projectConfigId, systemRowId);
            return data.map(ResponseEntity::ok)
                    .orElseGet(() -> {
                        // FIX: Return Map<String, Object> for consistency
                        Map<String, Object> errorBody = new HashMap<>();
                        errorBody.put("message", "Row with system_row_id '" + systemRowId + "' not found in table '" + logicalTableName + "'.");
                        errorBody.put("status", HttpStatus.NOT_FOUND.value());
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody);
                    });
        } catch (IllegalArgumentException e) {
            // Consistent error response
            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("message", e.getMessage());
            errorBody.put("status", HttpStatus.NOT_FOUND.value());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody);
        } catch (RuntimeException e) {
            // Consistent error response
            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("message", "Failed to retrieve data: " + e.getMessage());
            errorBody.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody);
        }
    }

    /**
     * Updates data in a dynamic table based on a filter column and value.
     *
     * @param projectConfigId The UUID of the project.
     * @param logicalTableName The logical name of the table.
     * @param filterColumn The column to use for filtering rows to update.
     * @param filterValue The value to match in the filterColumn. Changed to Object.
     * @param updateData A map of column names to new values.
     * @return ResponseEntity with update status.
     */
    @PutMapping("/{logicalTableName}/{filterColumn}/{filterValue}")
    public ResponseEntity<?> updateDynamicData(
            @PathVariable String projectConfigId,
            @PathVariable String logicalTableName,
            @PathVariable String filterColumn,
            @PathVariable Object filterValue,
            @RequestBody Map<String, Object> updateData) {
        try {
            int rowsAffected = tableCreationService.updateDataInDynamicTable(logicalTableName, projectConfigId, updateData, filterColumn, filterValue);
            if (rowsAffected > 0) {
                Map<String, Object> responseBody = new HashMap<>();
                responseBody.put("message", "Data updated successfully in table '" + logicalTableName + "'.");
                responseBody.put("rowsAffected", rowsAffected);
                return ResponseEntity.ok(responseBody);
            } else {
                Map<String, Object> errorBody = new HashMap<>();
                errorBody.put("message", "No rows found matching filter '" + filterColumn + "=" + filterValue + "' in table '" + logicalTableName + "'.");
                errorBody.put("status", HttpStatus.NOT_FOUND.value());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody);
            }
        } catch (IllegalArgumentException e) {
            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("message", e.getMessage());
            errorBody.put("status", HttpStatus.BAD_REQUEST.value());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody);
        } catch (RuntimeException e) {
            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("message", "Failed to update data: " + e.getMessage());
            errorBody.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody);
        }
    }

    /**
     * Deletes data from a dynamic table based on a filter column and value.
     *
     * @param projectConfigId The UUID of the project.
     * @param logicalTableName The logical name of the table.
     * @param filterColumn The column to use for filtering rows to delete.
     * @param filterValue The value to match in the filterColumn. Changed to Object.
     * @return ResponseEntity with delete status.
     */
    @DeleteMapping("/{logicalTableName}/{filterColumn}/{filterValue}")
    public ResponseEntity<?> deleteDynamicData(
            @PathVariable String projectConfigId,
            @PathVariable String logicalTableName,
            @PathVariable String filterColumn,
            @PathVariable Object filterValue) {
        try {
            int rowsAffected = tableCreationService.deleteDataFromDynamicTable(logicalTableName, projectConfigId, filterColumn, filterValue);
            if (rowsAffected > 0) {
                Map<String, Object> responseBody = new HashMap<>();
                responseBody.put("message", "Data deleted successfully from table '" + logicalTableName + "'.");
                responseBody.put("rowsAffected", rowsAffected);
                return ResponseEntity.ok(responseBody);
            } else {
                Map<String, Object> errorBody = new HashMap<>();
                errorBody.put("message", "No rows found matching filter '" + filterColumn + "=" + filterValue + "' in table '" + logicalTableName + "'.");
                errorBody.put("status", HttpStatus.NOT_FOUND.value());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody);
            }
        } catch (IllegalArgumentException e) {
            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("message", e.getMessage());
            errorBody.put("status", HttpStatus.BAD_REQUEST.value());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody);
        } catch (RuntimeException e) {
            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("message", "Failed to delete data: " + e.getMessage());
            errorBody.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody);
        }
    }
}