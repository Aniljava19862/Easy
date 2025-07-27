package com.easy.projectconfig.dto; // Create a new 'dto' package under projectconfig

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectConfigResponseDto {
    private String id; // The UUID
    private String projectName;
}