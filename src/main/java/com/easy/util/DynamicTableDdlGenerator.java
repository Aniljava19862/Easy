package com.easy.util;


import com.easy.tabledef.model.ColumnMetadata;
import com.easy.tabledef.model.TableMetadata;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class DynamicTableDdlGenerator {

    /**
     * Generates SQL for creating a table based on metadata.
     * Handles basic column types and primary keys.
     * Does NOT handle foreign keys or complex constraints for simplicity in skeleton.
     *
     * @param tableMetadata The metadata for the table to create.
     * @return SQL CREATE TABLE statement.
     */
    public String generateCreateTableSql(TableMetadata tableMetadata) {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE ").append(tableMetadata.getTableName()).append(" (");

        List<String> columnDefinitions = tableMetadata.getColumns().stream()
                .map(this::getColumnDefinition)
                .collect(Collectors.toList());

        // Add primary key constraint (assuming single primary key for simplicity)
        List<String> primaryKeys = tableMetadata.getColumns().stream()
                .filter(ColumnMetadata::isPrimaryKey)
                .map(ColumnMetadata::getColumnName)
                .collect(Collectors.toList());

        if (!primaryKeys.isEmpty()) {
            columnDefinitions.add("PRIMARY KEY (" + String.join(", ", primaryKeys) + ")");
        }

        sql.append(String.join(", ", columnDefinitions));
        sql.append(")");
        return sql.toString();
    }

    private String getColumnDefinition(ColumnMetadata column) {
        StringBuilder def = new StringBuilder();
        def.append(column.getColumnName()).append(" ");

        switch (column.getDataType().toUpperCase()) {
            case "VARCHAR":
                def.append("VARCHAR(").append(column.getLength() != null ? column.getLength() : 255).append(")");
                break;
            case "TEXT":
                def.append("TEXT");
                break;
            case "INTEGER":
                def.append("INTEGER");
                break;
            case "BIGINT":
                def.append("BIGINT");
                break;
            case "BOOLEAN":
                def.append("BOOLEAN");
                break;
            case "DATE":
                def.append("DATE");
                break;
            case "TIMESTAMP":
                def.append("TIMESTAMP");
                break;
            case "DECIMAL":
                def.append("DECIMAL"); // Or DECIMAL(precision, scale)
                break;
            // Add more data types as needed
            default:
                throw new IllegalArgumentException("Unsupported data type: " + column.getDataType());
        }

        if (!column.isNullable()) {
            def.append(" NOT NULL");
        }
        if (column.isUnique()) {
            def.append(" UNIQUE");
        }
        // TODO: Handle foreign keys. This requires joining with `TableMetadata` to get target table/column.
        // E.g., FOREIGN KEY (column_name) REFERENCES referenced_table(referenced_column_name)

        return def.toString();
    }

    public String generateDropTableSql(String tableName) {
        return "DROP TABLE IF EXISTS " + tableName;
    }

    // TODO: Add methods for ALTER TABLE (add column, drop column, modify column)
    // These are significantly more complex and require careful data migration strategies.
}
