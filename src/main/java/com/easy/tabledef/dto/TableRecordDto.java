package com.easy.tabledef.dto;

import java.util.List;
import java.util.Map;

public class TableRecordDto {
    private List<ColumnDefinitionDto> columnDefinitions; // Renamed from 'columns' for clarity
    private Map<String, Object> rowData;

    // Constructors
    public TableRecordDto() {
    }

    public TableRecordDto(List<ColumnDefinitionDto> columnDefinitions, Map<String, Object> rowData) {
        this.columnDefinitions = columnDefinitions;
        this.rowData = rowData;
    }

    // Getters
    public List<ColumnDefinitionDto> getColumnDefinitions() {
        return columnDefinitions;
    }

    public Map<String, Object> getRowData() {
        return rowData;
    }

    // Setters
    public void setColumnDefinitions(List<ColumnDefinitionDto> columnDefinitions) {
        this.columnDefinitions = columnDefinitions;
    }

    public void setRowData(Map<String, Object> rowData) {
        this.rowData = rowData;
    }

    @Override
    public String toString() {
        return "TableRecordDto{" +
                "columnDefinitions=" + columnDefinitions +
                ", rowData=" + rowData +
                '}';
    }
}