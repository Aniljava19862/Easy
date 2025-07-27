package com.easy.projectconfig.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data // Lombok: Generates getters, setters, toString, equals, hashCode
@NoArgsConstructor // Lombok: Generates a no-argument constructor
@AllArgsConstructor // Lombok: Generates an all-argument constructor
@JsonInclude(JsonInclude.Include.NON_NULL) // Exclude null fields from JSON serialization
public class MenuItem {
    private String label;
    private String iconPack;
    private String iconName;
    private String link; // Optional: Only present for leaf nodes

    // Recursive definition for children
    private List<MenuItem> children;

    // Custom constructor for convenience for non-children items
    public MenuItem(String label, String iconPack, String iconName, String link) {
        this.label = label;
        this.iconPack = iconPack;
        this.iconName = iconName;
        this.link = link;
        this.children = null; // Ensure children is null if not provided
    }
}