package com.easy.auth.model;

import jakarta.persistence.*;
import lombok.Data;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;

@Entity
@Table(name = "ui_designs")
@Data
public class UiDesign {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String description;
   // @Column(columnDefinition = "jsonb") // PostgreSQL specific JSONB type
    private String jsonDefinition; // Store JSON as String, let Jackson handle mapping
    private Long createdByUserId; // Link to User
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
