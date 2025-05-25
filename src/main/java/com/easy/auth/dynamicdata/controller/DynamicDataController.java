package com.easy.auth.dynamicdata.controller;


import com.easy.auth.dynamicdata.service.DynamicDataService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/data/{tableName}")
public class DynamicDataController {

    private final DynamicDataService dynamicDataService;

    public DynamicDataController(DynamicDataService dynamicDataService) {
        this.dynamicDataService = dynamicDataService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createRow(@PathVariable String tableName,
                                                         @RequestBody Map<String, Object> rowData) {
        Map<String, Object> createdRow = dynamicDataService.createRow(tableName, rowData);
        return new ResponseEntity<>(createdRow, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllRows(@PathVariable String tableName) {
        List<Map<String, Object>> rows = dynamicDataService.getAllRows(tableName);
        return ResponseEntity.ok(rows);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getRowById(@PathVariable String tableName, @PathVariable Long id) {
        Map<String, Object> row = dynamicDataService.getRowById(tableName, id);
        return ResponseEntity.ok(row);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateRow(@PathVariable String tableName, @PathVariable Long id,
                                          @RequestBody Map<String, Object> rowData) {
        dynamicDataService.updateRow(tableName, id, rowData);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRow(@PathVariable String tableName, @PathVariable Long id) {
        dynamicDataService.deleteRow(tableName, id);
        return ResponseEntity.noContent().build();
    }
}
