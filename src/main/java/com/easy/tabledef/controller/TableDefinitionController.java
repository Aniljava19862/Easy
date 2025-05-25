package com.easy.tabledef.controller;


import com.easy.tabledef.model.TableMetadata;
import com.easy.tabledef.service.TableDefinitionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/table-definitions")
public class TableDefinitionController {

    private final TableDefinitionService tableDefinitionService;

    public TableDefinitionController(TableDefinitionService tableDefinitionService) {
        this.tableDefinitionService = tableDefinitionService;
    }

    @PostMapping
    public ResponseEntity<TableMetadata> createTable(@RequestBody TableMetadata tableMetadata) {
        TableMetadata createdTable = tableDefinitionService.createTable(tableMetadata);
        return new ResponseEntity<>(createdTable, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<TableMetadata>> getAllTableDefinitions() {
        List<TableMetadata> tables = tableDefinitionService.getAllTableDefinitions();
        return ResponseEntity.ok(tables);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TableMetadata> getTableDefinitionById(@PathVariable Long id) {
        return tableDefinitionService.getTableDefinitionById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<TableMetadata> updateTable(@PathVariable Long id, @RequestBody TableMetadata tableMetadata) {
        TableMetadata updatedTable = tableDefinitionService.updateTable(id, tableMetadata);
        return ResponseEntity.ok(updatedTable);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTable(@PathVariable Long id) {
        tableDefinitionService.deleteTable(id);
        return ResponseEntity.noContent().build();
    }
}