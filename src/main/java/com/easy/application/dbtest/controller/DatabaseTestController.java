package com.easy.application.dbtest.controller; // Assuming your package path

import com.easy.application.dbtest.data.DatabaseConnectionDetails; // Adjust import
import com.easy.application.dbtest.service.DatabaseConnectivityService; // Adjust import
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
// Added back RequestMapping for clarity
public class DatabaseTestController {

    private final DatabaseConnectivityService connectivityService;

    // NO DataSource fields or injections in the constructor anymore!
    public DatabaseTestController(DatabaseConnectivityService connectivityService) {
        this.connectivityService = connectivityService;
    }

    /**
     * Endpoint for dynamically testing database connectivity with details provided in the request body.
     * This is the only way to test connectivity in this "no DataSource" setup.
     */
    @PostMapping(value = "/testDataSourceConnections", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> testDynamicConnection(
            @Valid @RequestBody DatabaseConnectionDetails details,
            BindingResult bindingResult) {

        // Handle validation errors first
        if (bindingResult.hasErrors()) {
            StringBuilder errors = new StringBuilder("Validation Failed: ");
            bindingResult.getFieldErrors().forEach(error ->
                    errors.append(error.getField()).append(": ").append(error.getDefaultMessage()).append("; ")
            );
            return ResponseEntity.badRequest().body(errors.toString());
        }

        // Call the service method that directly uses DriverManager
        String result = connectivityService.testConnectionDynamically(details);
        if (result.startsWith("SUCCESS")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    @PostMapping(value = "/createDataSourceConnections", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> createDataSourceConnections(
            @Valid @RequestBody DatabaseConnectionDetails details,
            BindingResult bindingResult) {

        // Handle validation errors first
        if (bindingResult.hasErrors()) {
            StringBuilder errors = new StringBuilder("Validation Failed: ");
            bindingResult.getFieldErrors().forEach(error ->
                    errors.append(error.getField()).append(": ").append(error.getDefaultMessage()).append("; ")
            );
            return ResponseEntity.badRequest().body(errors.toString());
        }

        // Call the service method that directly uses DriverManager
        String result = connectivityService.saveConnectionDetails(details);
        if (result.startsWith("SUCCESS")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }
    @GetMapping(value = "/getDataSourceConnections", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<DatabaseConnectionDetails>> getAllConnections() {
        List<DatabaseConnectionDetails> connections = connectivityService.getAllSavedConnections();
        if (connections.isEmpty()) {
            return ResponseEntity.noContent().build(); // 204 No Content if list is empty
        }
        return ResponseEntity.ok(connections); // 200 OK with the list of connections
    }

    /**
     * Endpoint to retrieve a specific saved database connection detail by its connection name.
     * Accessible via GET /db-test/connections/{connectionName}
     */
    @GetMapping(value = "/connections/{connectionName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DatabaseConnectionDetails> getConnectionByName(@PathVariable String connectionName) {
        Optional<DatabaseConnectionDetails> connection = connectivityService.getSavedConnectionByName(connectionName);
        return connection.map(ResponseEntity::ok) // If found, return 200 OK with the connection
                .orElseGet(() -> ResponseEntity.notFound().build()); // If not found, return 404 Not Found
    }
    // All @GetMapping methods for pre-configured DataSources are REMOVED (as per "no DataSource" requirement).
    // There are no pre-configured DataSources in this approach.
}