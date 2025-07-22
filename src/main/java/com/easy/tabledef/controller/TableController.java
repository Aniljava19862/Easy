package com.easy.tabledef.controller;

import com.easy.tabledef.model.TableDefinition;
import com.easy.tabledef.service.TableCreationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tables")
public class TableController {

    @Autowired
    private TableCreationService tableCreationService;

    @PostMapping("/create")
    public ResponseEntity<String> createTable(@RequestBody TableDefinition tableDefinition) {
        try {
            String result = tableCreationService.createTable(tableDefinition);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Validation Error: " + e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Server Error: " + e.getMessage());
        }
    }
}