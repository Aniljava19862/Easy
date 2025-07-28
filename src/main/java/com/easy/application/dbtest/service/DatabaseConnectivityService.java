package com.easy.application.dbtest.service;

import com.easy.application.dbtest.data.DatabaseConnectionDetails;
import com.easy.application.dbtest.repository.DatabaseConnectionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class DatabaseConnectivityService {

    @Autowired
    private final DatabaseConnectionRepository repository;



    public DatabaseConnectivityService(DatabaseConnectionRepository repository) {
        this.repository = repository;
    }

    private static final Map<String, String> DRIVER_CLASS_NAMES = new HashMap<>();
    private static final Map<String, String> JDBC_URL_TEMPLATES = new HashMap<>();
    private static final Map<String, String> TEST_QUERIES = new HashMap<>();

    static {
        DRIVER_CLASS_NAMES.put("mysql", "com.mysql.cj.jdbc.Driver");
        DRIVER_CLASS_NAMES.put("oracle", "oracle.jdbc.OracleDriver");
        DRIVER_CLASS_NAMES.put("postgresql", "org.postgresql.Driver");
        DRIVER_CLASS_NAMES.put("sqlserver", "com.microsoft.sqlserver.jdbc.SQLServerDriver");
        DRIVER_CLASS_NAMES.put("h2", "org.h2.Driver");

        JDBC_URL_TEMPLATES.put("mysql", "jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true");
        JDBC_URL_TEMPLATES.put("oracle", "jdbc:oracle:thin:@%s:%d:%s");
        JDBC_URL_TEMPLATES.put("postgresql", "jdbc:postgresql://%s:%d/%s");
        JDBC_URL_TEMPLATES.put("sqlserver", "jdbc:sqlserver://%s:%d;databaseName=%s;encrypt=false;trustServerCertificate=true;");
        JDBC_URL_TEMPLATES.put("h2", "jdbc:h2:mem:%s;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");

        TEST_QUERIES.put("mysql", "SELECT 1");
        TEST_QUERIES.put("oracle", "SELECT 1 FROM DUAL");
        TEST_QUERIES.put("postgresql", "SELECT 1");
        TEST_QUERIES.put("sqlserver", "SELECT 1");
        TEST_QUERIES.put("h2", "SELECT 1");
    }

    public String testConnectionDynamically(DatabaseConnectionDetails details) {
        String dbType = details.getDbType().toLowerCase();
        String driverClass = DRIVER_CLASS_NAMES.get(dbType);
        String urlTemplate = JDBC_URL_TEMPLATES.get(dbType);
        String testQuery = TEST_QUERIES.get(dbType);

        if (driverClass == null || urlTemplate == null || testQuery == null) {
            return "FAILURE: Unsupported database type: " + details.getDbType();
        }

        try {
            Class.forName(driverClass);
        } catch (ClassNotFoundException e) {
            return "FAILURE: JDBC Driver not found for " + details.getDbType() + ". Error: " + e.getMessage();
        }

        String jdbcUrl;
        if ("h2".equals(dbType) && details.getDataBasePort() == 0) {
            jdbcUrl = String.format(urlTemplate, details.getConnectionName());
        } else {
            int port = details.getDataBasePort() > 0 ? details.getDataBasePort() : getDefaultPort(dbType);
            jdbcUrl = String.format(urlTemplate, details.getDataBaseIp(), port, details.getConnectionName());
        }

        try (Connection connection = DriverManager.getConnection(jdbcUrl, details.getDbUserName(), details.getDbPassword())) {
            try (Statement statement = connection.createStatement()) {
                statement.execute(testQuery);
            }
            return "SUCCESS: Connected to " + details.getDbType() + " database at " + details.getDataBaseIp() + ":" + details.getDataBasePort() + "/" + details.getConnectionName();
        } catch (SQLException e) {
            return "FAILURE: Could not connect to " + details.getDbType() + " database. Error: " + e.getMessage();
        } catch (Exception e) {
            return "FAILURE: Error during setup for " + details.getDbType() + " database. Error: " + e.getMessage();
        }
    }

    // Changed return type from String to DatabaseConnectionDetails
    @Transactional
    public DatabaseConnectionDetails saveConnectionDetails(DatabaseConnectionDetails details) {
        String testResult = testConnectionDynamically(details);
        if (testResult.startsWith("FAILURE")) {
            // Throw an exception for failed connection test
            throw new IllegalArgumentException("Cannot save invalid connection. Test failed: " + testResult.substring("FAILURE: ".length()));
        }

        Optional<DatabaseConnectionDetails> existingConnection = repository.findByConnectionName(details.getConnectionName());
        if (existingConnection.isPresent()) {
            throw new IllegalArgumentException("A connection with the name '" + details.getConnectionName() + "' already exists. Please choose a different name.");
        }

        // The @PrePersist method in DatabaseConnectionDetails will generate the UUID
        return repository.save(details); // Returns the saved entity with the generated ID and UUID
    }

    @Transactional(readOnly = true)
    public List<DatabaseConnectionDetails> getAllSavedConnections() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<DatabaseConnectionDetails> getSavedConnectionByName(String connectionName) {
        return repository.findByConnectionName(connectionName);
    }

    // --- NEW METHOD TO GET BY UUID ---
    @Transactional(readOnly = true)
    public Optional<DatabaseConnectionDetails> getSavedConnectionByUuid(String uuid) {
        return repository.findByUuid(uuid);
    }

    private int getDefaultPort(String dbType) {
        return switch (dbType) {
            case "mysql" -> 3306;
            case "oracle" -> 1521;
            case "postgresql" -> 5432;
            case "sqlserver" -> 1433;
            case "h2" -> 0;
            default -> 0;
        };
    }

    /**
     * Retrieves database connection details by its ID.
     * THIS IS THE MISSING METHOD.
     * @param id The ID of the database connection details.
     * @return An Optional containing the DatabaseConnectionDetails if found.
     */
    @Transactional(readOnly = true)
    public Optional<DatabaseConnectionDetails> getDatabaseConnectionDetails(String id) {
        return repository.findByUuid(id);
    }

}