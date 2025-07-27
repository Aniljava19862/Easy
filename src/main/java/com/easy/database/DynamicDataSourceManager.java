package com.easy.database; // Create a new package for database-related utilities

import com.easy.application.dbtest.data.DatabaseConnectionDetails;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;
import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DynamicDataSourceManager {

    private final Map<String, DataSource> dataSourceCache = new ConcurrentHashMap<>();
    private final Map<String, JdbcTemplate> jdbcTemplateCache = new ConcurrentHashMap<>();

    private static final Map<String, String> DRIVER_CLASS_NAMES = new ConcurrentHashMap<>();
    private static final Map<String, String> JDBC_URL_TEMPLATES = new ConcurrentHashMap<>();

    static {
        DRIVER_CLASS_NAMES.put("mysql", "com.mysql.cj.jdbc.Driver");
        DRIVER_CLASS_NAMES.put("oracle", "oracle.jdbc.OracleDriver");
        DRIVER_CLASS_NAMES.put("postgresql", "org.postgresql.Driver");
        DRIVER_CLASS_NAMES.put("sqlserver", "com.microsoft.sqlserver.jdbc.SQLServerDriver");
        DRIVER_CLASS_NAMES.put("h2", "org.h2.Driver"); // For in-memory H2 testing
        // Add more database types as needed

        JDBC_URL_TEMPLATES.put("mysql", "jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        JDBC_URL_TEMPLATES.put("oracle", "jdbc:oracle:thin:@%s:%d:%s"); // SID or Service Name
        JDBC_URL_TEMPLATES.put("postgresql", "jdbc:postgresql://%s:%d/%s");
        JDBC_URL_TEMPLATES.put("sqlserver", "jdbc:sqlserver://%s:%d;databaseName=%s;encrypt=false;trustServerCertificate=true;");
        JDBC_URL_TEMPLATES.put("h2", "jdbc:h2:mem:%s;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"); // H2 example
        // Add more URL templates
    }

    public DataSource getDataSource(DatabaseConnectionDetails details) {
        String cacheKey = details.getUuid();

        return dataSourceCache.computeIfAbsent(cacheKey, k -> {
            HikariConfig config = new HikariConfig();

            String dbType = details.getDbType().toLowerCase();
            String driverClass = DRIVER_CLASS_NAMES.get(dbType);
            String urlTemplate = JDBC_URL_TEMPLATES.get(dbType);

            if (driverClass == null || urlTemplate == null) {
                throw new IllegalArgumentException("Unsupported database type or missing driver/URL template: " + details.getDbType());
            }

            try {
                Class.forName(driverClass); // Load driver
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException("JDBC Driver not found for " + details.getDbType() + ": " + driverClass, e);
            }

            String jdbcUrl;
            if ("h2".equals(dbType) && details.getDataBasePort() == 0) {
                jdbcUrl = String.format(urlTemplate, details.getConnectionName());
            } else {
                int port = details.getDataBasePort() > 0 ? details.getDataBasePort() : getDefaultPort(dbType);
                jdbcUrl = String.format(urlTemplate, details.getDataBaseIp(), port, details.getConnectionName());
            }

            config.setJdbcUrl(jdbcUrl);
            config.setUsername(details.getDbUserName());
            config.setPassword(details.getDbPassword());
            config.setDriverClassName(driverClass);

            // HikariCP connection pool properties (customize as needed)
            config.setMinimumIdle(1);
            config.setMaximumPoolSize(10);
            config.setIdleTimeout(30000); // 30 seconds
            config.setConnectionTimeout(30000); // 30 seconds
            config.setMaxLifetime(600000); // 10 minutes

            System.out.println("Creating new HikariDataSource for: " + details.getConnectionName());
            return new HikariDataSource(config);
        });
    }

    public JdbcTemplate getJdbcTemplate(DatabaseConnectionDetails details) {
        String cacheKey = details.getUuid();
        return jdbcTemplateCache.computeIfAbsent(cacheKey, k -> new JdbcTemplate(getDataSource(details)));
    }

    private int getDefaultPort(String dbType) {
        return switch (dbType) {
            case "mysql" -> 3306;
            case "oracle" -> 1521;
            case "postgresql" -> 5432;
            case "sqlserver" -> 1433;
            case "h2" -> 0;
            default -> 0; // Or throw exception for unknown type
        };
    }

    public void closeDataSource(String uuid) {
        DataSource dataSource = dataSourceCache.remove(uuid);
        if (dataSource instanceof HikariDataSource) {
            ((HikariDataSource) dataSource).close();
            System.out.println("Closed HikariDataSource for UUID: " + uuid);
        }
        jdbcTemplateCache.remove(uuid);
    }

    public void closeAllDataSources() {
        dataSourceCache.forEach((uuid, dataSource) -> {
            if (dataSource instanceof HikariDataSource) {
                ((HikariDataSource) dataSource).close();
                System.out.println("Closed HikariDataSource for UUID: " + uuid);
            }
        });
        dataSourceCache.clear();
        jdbcTemplateCache.clear();
        System.out.println("All cached DataSources closed.");
    }
}