package com.easy.menu.service;

import com.easy.menu.model.MenuLinkConfig;
import com.easy.menu.repository.MenuLinkConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class MenuLinkConfigService {

    @Autowired
    private MenuLinkConfigRepository menuLinkConfigRepository;

    /**
     * Creates a new menu link configuration.
     * @param tableDefinitionId The ID of the TableDefinition this link is for.
     * @param projectConfigId The ID of the ProjectConfig this link belongs to.
     * @param linkName The display name for the menu link.
     * @param logicalTableName The logical table name used to generate the path.
     * @return The created MenuLinkConfig object.
     */
    @Transactional
    public MenuLinkConfig createMenuLink(String tableDefinitionId, String projectConfigId, String linkName, String logicalTableName) {
        // Construct a default linkPath based on your frontend routing convention
        // Example: /projects/{projectId}/data/{logicalTableName}
        String linkPath = String.format("/projects/%s/data/%s", projectConfigId, logicalTableName);

        MenuLinkConfig menuLink = MenuLinkConfig.builder()
                .id(UUID.randomUUID().toString())
                .tableDefinitionIdRef(tableDefinitionId)
                .projectConfigIdRef(projectConfigId)
                .linkName(linkName)
                .linkPath(linkPath)
                .menuOrder(0) // Default order, can be customized later if needed
                .iconClass("fa fa-table") // Default icon, change as per your icon library
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return menuLinkConfigRepository.save(menuLink);
    }

    /**
     * Retrieves a menu link configuration by the ID of the table definition it refers to.
     * @param tableDefinitionId The ID of the TableDefinition.
     * @return An Optional containing the MenuLinkConfig if found.
     */
    @Transactional(readOnly = true)
    public Optional<MenuLinkConfig> getMenuLinkByTableDefinitionId(String tableDefinitionId) {
        return menuLinkConfigRepository.findByTableDefinitionIdRef(tableDefinitionId);
    }

    /**
     * Retrieves all menu links for a specific project, ordered by their display order.
     * @param projectConfigId The ID of the project configuration.
     * @return A list of MenuLinkConfig objects.
     */
    @Transactional(readOnly = true)
    public List<MenuLinkConfig> getMenuLinksByProject(String projectConfigId) {
        return menuLinkConfigRepository.findByProjectConfigIdRefOrderByMenuOrderAsc(projectConfigId);
    }

    /**
     * Deletes a menu link configuration associated with a given table definition ID.
     * @param tableDefinitionId The ID of the TableDefinition whose menu link should be deleted.
     */
    @Transactional
    public void deleteMenuLinkByTableDefinitionId(String tableDefinitionId) {
        menuLinkConfigRepository.findByTableDefinitionIdRef(tableDefinitionId).ifPresent(
                menuLinkConfigRepository::delete
        );
    }
}