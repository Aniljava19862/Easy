package com.easy.tabledef.controller;

import com.easy.tabledef.dto.TableDataResponseDto;
import com.easy.tabledef.dto.TableDefinitionDto;
import com.easy.tabledef.model.TableDefinition;
import com.easy.tabledef.service.TableCreationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/{projectConfigId}")
public class TableController {

    @Autowired
    private TableCreationService tableCreationService;

    /**
     * Creates a new dynamic table and its definition.
     *
     * @param projectConfigId The UUID of the project.
     * @param tableDefinition The table definition to create.
     * @return ResponseEntity with the created TableDefinitionDto or an error message.
     */
    @PostMapping(value = "/createtable", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createTable(
            @PathVariable String projectConfigId,
            @RequestBody TableDefinition tableDefinition) {
        try {
            TableDefinitionDto createdTableDto = tableCreationService.createTable(tableDefinition, projectConfigId);
            // Changed return type to TableDefinitionDto for success
            return ResponseEntity.status(HttpStatus.CREATED).body(createdTableDto);
        } catch (IllegalArgumentException e) {
            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("message", e.getMessage());
            errorBody.put("status", HttpStatus.BAD_REQUEST.value());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody);
        } catch (RuntimeException e) {
            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("message", "Failed to create table: " + e.getMessage());
            errorBody.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody);
        }
    }

    /**
     * Retrieves all table definitions for a specific project.
     *
     * @param projectConfigId The UUID of the project.
     * @return ResponseEntity with a list of TableDefinitionDto or an error message.
     */
    @GetMapping
    public ResponseEntity<?> getAllTableDefinitions(@PathVariable String projectConfigId) {
        try {
            // Corrected method name call
            List<TableDefinitionDto> tableDefs = tableCreationService.getAllTableDefinitionsForProject(projectConfigId);
            if (tableDefs.isEmpty()) {
                Map<String, Object> errorBody = new HashMap<>();
                errorBody.put("message", "No table definitions found for project '" + projectConfigId + "'.");
                errorBody.put("status", HttpStatus.NOT_FOUND.value());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody);
            }
            return ResponseEntity.ok(tableDefs);
        } catch (IllegalArgumentException e) {
            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("message", e.getMessage());
            errorBody.put("status", HttpStatus.NOT_FOUND.value());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody);
        } catch (RuntimeException e) {
            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("message", "Failed to retrieve table definitions: " + e.getMessage());
            errorBody.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody);
        }
    }

    /**
     * Retrieves a specific table definition by its ID.
     *
     * @param projectConfigId The UUID of the project.
     * @param tableDefinitionId The UUID of the table definition.
     * @return ResponseEntity with the TableDefinitionDto or not found.
     */
    @GetMapping("/{tableDefinitionId}")
    public ResponseEntity<?> getTableDefinitionById(
            @PathVariable String projectConfigId,
            @PathVariable String tableDefinitionId) {
        try {
            // Assuming tableCreationService has a getTableDefinitionById method (or similar)
            // If not, you might need to add one or use the repository directly if appropriate.
            // For now, I'll assume TableCreationService can fetch it via its repo.
            Optional<TableDefinition> tableDef = tableCreationService.getTableDefinitionByLogicalNameAndProject(null, projectConfigId); // Placeholder, assuming logicalTableName is not used here
            // This is a placeholder as tableCreationService currently only has findByLogicalNameAndProject.
            // You might need a method like `tableDefinitionRepository.findById(tableDefinitionId)` directly
            // or add a specific service method for it.
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body("Fetching table definition by ID is not yet fully implemented via service.");

        } catch (IllegalArgumentException e) {
            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("message", e.getMessage());
            errorBody.put("status", HttpStatus.NOT_FOUND.value());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody);
        } catch (RuntimeException e) {
            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("message", "Failed to retrieve table definition: " + e.getMessage());
            errorBody.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody);
        }
    }


    /**
     * Updates an existing table definition.
     *
     * @param projectConfigId The UUID of the project.
     * @param tableDefinitionId The ID of the table definition to update.
     * @param updatedDefinition The updated table definition data.
     * @return ResponseEntity with the updated TableDefinitionDto or an error message.
     */
    @PutMapping("/{tableDefinitionId}")
    public ResponseEntity<?> updateTableDefinition(
            @PathVariable String projectConfigId,
            @PathVariable String tableDefinitionId,
            @RequestBody TableDefinition updatedDefinition) {
        try {
            TableDefinitionDto resultDto = tableCreationService.updateTableDefinition(tableDefinitionId, updatedDefinition, projectConfigId);
            // Changed return type to TableDefinitionDto for success
            return ResponseEntity.ok(resultDto);
        } catch (IllegalArgumentException e) {
            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("message", e.getMessage());
            errorBody.put("status", HttpStatus.BAD_REQUEST.value());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody);
        } catch (RuntimeException e) {
            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("message", "Failed to update table definition: " + e.getMessage());
            errorBody.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody);
        }
    }

    /**
     * Deletes a table definition and its associated physical table.
     *
     * @param projectConfigId The UUID of the project.
     * @param tableDefinitionId The ID of the table definition to delete.
     * @return ResponseEntity with success message or error.
     */
    @DeleteMapping("/{tableDefinitionId}")
    public ResponseEntity<?> deleteTableDefinition(
            @PathVariable String projectConfigId,
            @PathVariable String tableDefinitionId) {
        try {
            // Assuming a deleteTable method in service exists or will be implemented
            // For now, returning NOT_IMPLEMENTED for this.
            // tableCreationService.deleteTable(tableDefinitionId, projectConfigId);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body("Table deletion is not yet implemented.");
        } catch (IllegalArgumentException e) {
            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("message", e.getMessage());
            errorBody.put("status", HttpStatus.BAD_REQUEST.value());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody);
        } catch (RuntimeException e) {
            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("message", "Failed to delete table definition: " + e.getMessage());
            errorBody.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody);
        }
    }

    // New Endpoint based on error: getTableDefinitionAndSingleRow
    @GetMapping("/{logicalTableName}/{systemRowId}/with-definition")
    public ResponseEntity<?> getTableDefinitionAndSingleRow(
            @PathVariable String projectConfigId,
            @PathVariable String logicalTableName,
            @PathVariable String systemRowId) {
        try {
            Optional<Map<String, Object>> result = tableCreationService.getTableDefinitionAndSingleRow(logicalTableName, projectConfigId, systemRowId);
            return result.map(ResponseEntity::ok)
                    .orElseGet(() -> {
                        Map<String, Object> errorBody = new HashMap<>();
                        errorBody.put("message", "Table definition or row not found for table '" + logicalTableName + "' and system_row_id '" + systemRowId + "'.");
                        errorBody.put("status", HttpStatus.NOT_FOUND.value());
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody);
                    });
        } catch (IllegalArgumentException e) {
            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("message", e.getMessage());
            errorBody.put("status", HttpStatus.BAD_REQUEST.value());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody);
        } catch (RuntimeException e) {
            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("message", "Failed to retrieve data: " + e.getMessage());
            errorBody.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody);
        }
    }

    // --- NOTE TO USER: Regarding getFilteredDataFromDynamicTable ---
    // The error "cannot find symbol method getFilteredDataFromDynamicTable"
    // suggests a method that doesn't exist in TableCreationService.
    // You'll need to either:
    // 1. Remove the call to this method if it's no longer needed.
    // 2. Implement the method in TableCreationService if you need generic filtering.
    //    (e.g., `List<Map<String, Object>> getFilteredData(String logicalTableName, String projectConfigId, Map<String, Object> filters)`
    //    which would build a dynamic WHERE clause based on the filters map).
    // I'm commenting out a placeholder for this to prevent compilation errors.
    /*
    @GetMapping("/{logicalTableName}/filtered")
    public ResponseEntity<?> getFilteredData(
            @PathVariable String projectConfigId,
            @PathVariable String logicalTableName,
            @RequestParam Map<String, Object> filters) {
        try {
            // This method needs to be implemented in TableCreationService
            // List<Map<String, Object>> data = tableCreationService.getFilteredDataFromDynamicTable(logicalTableName, projectConfigId, filters);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body("Filtered data retrieval not yet implemented.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to retrieve filtered data: " + e.getMessage());
        }
    }
    */



    /**
     * Retrieves all rows from a specific dynamic database table along with its column definitions.
     * GET /api/projects/{projectConfigId}/table-definitions/{logicalTableName}/data
     *
     * @param projectConfigId The UUID of the project.
     * @param logicalTableName The logical name of the table whose data is to be retrieved.
     * @return ResponseEntity with a TableDataResponseDto containing column definitions and all rows of data, or an error message.
     */
    @GetMapping("/{logicalTableName}/data")
    public ResponseEntity<?> getCombinedTableData(
            @PathVariable String projectConfigId,
            @PathVariable String logicalTableName) {
        try {
            TableDataResponseDto responseDto = tableCreationService.getCombinedTableData(logicalTableName, projectConfigId);

            // Check if rowData is empty, and provide a message if needed (though 200 OK with empty list is typical)
            if (responseDto.getRowData().isEmpty()) {
                // You can choose to return 200 OK with an empty data array and a message,
                // or keep it just as the DTO directly.
                // The current DTO structure allows an empty rowData list naturally.
                // For a more explicit message on empty data, you might structure the response differently.
                // Sticking to returning the DTO as is for simplicity, client can check rowData.isEmpty().
                return ResponseEntity.ok(responseDto);
            }

            return ResponseEntity.ok(responseDto);
        } catch (IllegalArgumentException e) {
            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("message", e.getMessage());
            errorBody.put("status", HttpStatus.BAD_REQUEST.value());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody);
        } catch (RuntimeException e) {
            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("message", "Failed to retrieve combined table data: " + e.getMessage());
            errorBody.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody);
        }
    }
}