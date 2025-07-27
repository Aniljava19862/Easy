package com.easy.application.dbtest.controller; // Assuming your package path

import com.easy.application.dbtest.data.DatabaseConnectionDetails; // Adjust import
import com.easy.application.dbtest.service.DatabaseConnectivityService; // Adjust import
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@RestController
public class DatabaseTestController {

    private final DatabaseConnectivityService connectivityService;

    public DatabaseTestController(DatabaseConnectivityService connectivityService) {
        this.connectivityService = connectivityService;
    }

    /**
     * Endpoint for dynamically testing database connectivity with details provided in the request body.
     * Original URL: /testDataSourceConnections
     */
    @PostMapping(value = "/testDataSourceConnections", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> testDynamicConnection(
            @Valid @RequestBody DatabaseConnectionDetails details,
            BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            StringBuilder errors = new StringBuilder("Validation Failed: ");
            bindingResult.getFieldErrors().forEach(error ->
                    errors.append(error.getField()).append(": ").append(error.getDefaultMessage()).append("; ")
            );
            return ResponseEntity.badRequest().body(errors.toString());
        }

        String result = connectivityService.testConnectionDynamically(details);
        if (result.startsWith("SUCCESS")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * Endpoint for creating/saving database connection details.
     * Original URL: /createDataSourceConnections
     * Returns the saved DatabaseConnectionDetails object, including the generated UUID.
     */
    @PostMapping(value = "/createDataSourceConnections", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DatabaseConnectionDetails> createDataSourceConnections(
            @Valid @RequestBody DatabaseConnectionDetails details,
            BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            StringBuilder errors = new StringBuilder("Validation Failed: ");
            bindingResult.getFieldErrors().forEach(error ->
                    errors.append(error.getField()).append(": ").append(error.getDefaultMessage()).append("; ")
            );
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errors.toString());
        }

        try {
            DatabaseConnectionDetails savedDetails = connectivityService.saveConnectionDetails(details);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedDetails); // 201 Created
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save connection: " + e.getMessage());
        }
    }

    /**
     * Endpoint to get all saved database connection details.
     * Original URL: /getDataSourceConnections
     */
    @GetMapping(value = "/getDataSourceConnections", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<DatabaseConnectionDetails>> getAllConnections() {
        List<DatabaseConnectionDetails> connections = connectivityService.getAllSavedConnections();
        if (connections.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(connections);
    }

    /**
     * Endpoint to retrieve a specific saved database connection detail by its connection name.
     * Original URL: /connections/{connectionName}
     */
    @GetMapping(value = "/connections/{connectionName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DatabaseConnectionDetails> getConnectionByName(@PathVariable String connectionName) {
        Optional<DatabaseConnectionDetails> connection = connectivityService.getSavedConnectionByName(connectionName);
        return connection.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * NEW Endpoint to retrieve a specific saved database connection detail by its UUID.
     * This uses a new path to avoid conflicts with existing paths.
     * URL: /getDataSourceConnections/uuid/{uuid}
     */
    @GetMapping(value = "/getDataSourceConnections/uuid/{uuid}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DatabaseConnectionDetails> getConnectionByUuid(@PathVariable String uuid) {
        Optional<DatabaseConnectionDetails> connection = connectivityService.getSavedConnectionByUuid(uuid);
        return connection.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}