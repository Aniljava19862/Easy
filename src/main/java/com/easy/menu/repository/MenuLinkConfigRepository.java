package com.easy.menu.repository;

import com.easy.menu.model.MenuLinkConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MenuLinkConfigRepository extends JpaRepository<MenuLinkConfig, String> {
    // Find a menu link by the ID of the table definition it refers to
    Optional<MenuLinkConfig> findByTableDefinitionIdRef(String tableDefinitionIdRef);

    // Find all menu links for a given project, ordered by their display order
    List<MenuLinkConfig> findByProjectConfigIdRefOrderByMenuOrderAsc(String projectConfigIdRef);
}