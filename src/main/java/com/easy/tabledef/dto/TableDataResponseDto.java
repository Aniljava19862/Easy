package com.easy.tabledef.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TableDataResponseDto {
    private List<ColumnDefinitionDto> columnDefinitions;
    private List<Map<String, Object>> rowData; // Changed to List<Map<String, Object>> for multiple rows
}