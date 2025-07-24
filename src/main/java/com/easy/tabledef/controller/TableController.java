package com.easy.tabledef.controller;

import com.easy.tabledef.dto.TableDefinitionDto;
import com.easy.tabledef.dto.TableRecordDto;
import com.easy.tabledef.model.TableDefinition;
import com.easy.tabledef.service.TableCreationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional; // Import Optional

@RestController
@RequestMapping("/tables")
public class TableController {

    @Autowired
    private TableCreationService tableCreationService;

    /**
     * Endpoint to create a new dynamic table and save its definition.
     * POST /api/tables/create
     *
     * @param tableDefinition The JSON request body containing table and column definitions.
     * @return ResponseEntity indicating success or failure.
     */
    @PostMapping("/create")
    public ResponseEntity<String> createTable(@RequestBody TableDefinition tableDefinition) {
        try {
            String result = tableCreationService.createTable(tableDefinition);
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Validation Error: " + e.getMessage());
        } catch (RuntimeException e) {
            e.printStackTrace(); // Log the full stack trace for debugging
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Server Error: " + e.getMessage());
        }
    }

    /**
     * Endpoint to get a table definition by its logical name.
     * GET /api/tables/{tableName}
     *
     * @param tableName The logical name of the table.
     * @return ResponseEntity with TableDefinition on success, or String message on 404.
     */
    @GetMapping("/{tableName}")
    public ResponseEntity<?> getTableDefinitionByLogicalName(@PathVariable String tableName) {
        Optional<TableDefinition> tableDefOptional = tableCreationService.getTableDefinitionByLogicalName(tableName);
        if (tableDefOptional.isPresent()) {
            return ResponseEntity.ok(tableDefOptional.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Table definition with logical name '" + tableName + "' not found.");
        }
    }

    /**
     * Endpoint to get a table definition by its physical (final) table name.
     * GET /api/tables/physical/{finalTableName}
     *
     * @param finalTableName The physical name of the table in the database.
     * @return ResponseEntity with TableDefinition on success, or String message on 404.
     */
    @GetMapping("/physical/{finalTableName}")
    public ResponseEntity<?> getTableDefinitionByPhysicalName(@PathVariable String finalTableName) {
        Optional<TableDefinition> tableDefOptional = tableCreationService.getTableDefinitionByPhysicalName(finalTableName);
        if (tableDefOptional.isPresent()) {
            return ResponseEntity.ok(tableDefOptional.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Table definition for physical table name '" + finalTableName + "' not found.");
        }
    }

    // --- Dynamic Table Data Operations ---

    /**
     * Inserts data into a dynamically created table.
     * POST /api/tables/data/{logicalTableName}
     *
     * @param logicalTableName The logical name of the table (as stored in TableDefinition).
     * @param data             A JSON object representing the row data (columnName: value).
     * @return ResponseEntity with success or error message.
     */
    @PostMapping("/data/{logicalTableName}")
    public ResponseEntity<String> insertDynamicTableData(@PathVariable String logicalTableName, @RequestBody Map<String, Object> data) {
        try {
            int rowsAffected = tableCreationService.addDataToDynamicTable(logicalTableName, data);
            return ResponseEntity.status(HttpStatus.CREATED).body(rowsAffected + " row(s) inserted into '" + logicalTableName + "'.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Validation Error: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error inserting data into table '" + logicalTableName + "': " + e.getMessage());
        }
    }

    /**
     * Retrieves all data from a dynamically created table.
     * GET /api/tables/data/{logicalTableName}
     *
     * @param logicalTableName The logical name of the table.
     * @return ResponseEntity with a list of maps representing table rows.
     */
    @GetMapping("/data/{logicalTableName}")
    public ResponseEntity<?> getDynamicTableData(@PathVariable String logicalTableName) {
        try {
            List<Map<String, Object>> data = tableCreationService.getAllDataFromDynamicTable(logicalTableName);
            if (data.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT).body("No data found in table '" + logicalTableName + "'.");
            }
            return ResponseEntity.ok(data);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Validation Error: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving data from table '" + logicalTableName + "': " + e.getMessage());
        }
    }

    /**
     * Retrieves data from a dynamically created table with a single filter condition.
     * GET /api/tables/data/{logicalTableName}/filter/{filterColumn}/{filterValue}
     *
     * @param logicalTableName The logical name of the table.
     * @param filterColumn     The column to filter by.
     * @param filterValue      The value to match. Note: Path variables are strings; consider type conversion in service if needed.
     * @return ResponseEntity with a list of maps representing filtered table rows.
     */
    @GetMapping("/data/{logicalTableName}/filter/{filterColumn}/{filterValue}")
    public ResponseEntity<?> getDynamicTableDataFiltered(
            @PathVariable String logicalTableName,
            @PathVariable String filterColumn,
            @PathVariable String filterValue) { // PathVariable comes as String, conversion happens in service if needed
        try {
            List<Map<String, Object>> data = tableCreationService.getFilteredDataFromDynamicTable(logicalTableName, filterColumn, filterValue);
            if (data.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT).body("No data found for filter: " + filterColumn + " = " + filterValue);
            }
            return ResponseEntity.ok(data);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Validation Error: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving filtered data from table '" + logicalTableName + "': " + e.getMessage());
        }
    }


    /**
     * Updates data in a dynamically created table based on a single condition.
     * PUT /api/tables/data/{logicalTableName}/{filterColumn}/{filterValue}
     *
     * @param logicalTableName The logical name of the table.
     * @param filterColumn     The column to use for the WHERE clause.
     * @param filterValue      The value for the WHERE clause.
     * @param updateData       A JSON object with columns and new values to update.
     * @return ResponseEntity with success or error message.
     */
    @PutMapping("/data/{logicalTableName}/{filterColumn}/{filterValue}")
    public ResponseEntity<String> updateDynamicTableData(
            @PathVariable String logicalTableName,
            @PathVariable String filterColumn,
            @PathVariable String filterValue, // PathVariable comes as String
            @RequestBody Map<String, Object> updateData) {
        try {
            int rowsAffected = tableCreationService.updateDataInDynamicTable(logicalTableName, updateData, filterColumn, filterValue);
            return ResponseEntity.ok(rowsAffected + " row(s) updated in '" + logicalTableName + "'.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Validation Error: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error updating data in table '" + logicalTableName + "': " + e.getMessage());
        }
    }

    /**
     * Deletes data from a dynamically created table based on a single condition.
     * DELETE /api/tables/data/{logicalTableName}/{filterColumn}/{filterValue}
     *
     * @param logicalTableName The logical name of the table.
     * @param filterColumn     The column to use for the WHERE clause.
     * @param filterValue      The value for the WHERE clause.
     * @return ResponseEntity with success or error message.
     */
    @DeleteMapping("/data/{logicalTableName}/{filterColumn}/{filterValue}")
    public ResponseEntity<String> deleteDynamicTableData(
            @PathVariable String logicalTableName,
            @PathVariable String filterColumn,
            @PathVariable String filterValue) { // PathVariable comes as String
        try {
            int rowsAffected = tableCreationService.deleteDataFromDynamicTable(logicalTableName, filterColumn, filterValue);
            return ResponseEntity.ok(rowsAffected + " row(s) deleted from '" + logicalTableName + "'.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Validation Error: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error deleting data from table '" + logicalTableName + "': " + e.getMessage());
        }
    }

    @GetMapping("/list")
    public ResponseEntity<List<TableDefinitionDto>> getAllTableDefinitions() {
        List<TableDefinitionDto> tableDefinitions = tableCreationService.getAllTableDefinitions();
        if (tableDefinitions.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body(List.of()); // Return 204 No Content if no tables
        }
        return ResponseEntity.ok(tableDefinitions); // Return 200 OK with the list of tables
    }

    // --- NEW ENDPOINT: Get Combined Table Definition and Single Row Data ---
    /**
     * Retrieves the definition and a single row of data for a dynamic table.
     * This is useful for editing purposes where the frontend needs both schema and data.
     * GET /tables/data/{logicalTableName}/record/{systemRowId}
     *
     * @param logicalTableName The logical name of the table.
     * @param systemRowId      The system-generated UUID of the row to fetch.
     * @return ResponseEntity with a TableRecordDto on success, or an error/404 message.
     */
    @GetMapping("/data/{logicalTableName}/record/{systemRowId}")
    public ResponseEntity<?> getTableDefinitionAndSingleRecord(
            @PathVariable String logicalTableName,
            @PathVariable String systemRowId) {
        try {
            Optional<TableRecordDto> tableRecordDtoOptional =
                    tableCreationService.getTableDefinitionAndSingleRow(logicalTableName, systemRowId);

            if (tableRecordDtoOptional.isPresent()) {
                return ResponseEntity.ok(tableRecordDtoOptional.get());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Row with system ID '" + systemRowId + "' not found in table '" + logicalTableName + "'.");
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Validation Error: " + e.getMessage());
        } catch (RuntimeException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Server Error: " + e.getMessage());
        }
    }

}