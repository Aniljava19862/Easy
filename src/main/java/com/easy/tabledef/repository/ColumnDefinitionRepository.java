package com.easy.tabledef.repository;

import com.easy.tabledef.model.ColumnDefinition;
import com.easy.tabledef.model.TableDefinition; // Import TableDefinition for custom query
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ColumnDefinitionRepository extends JpaRepository<ColumnDefinition, String> {

    /**
     * Finds all ColumnDefinition entities associated with a given TableDefinition.
     * This is useful for managing columns when a TableDefinition is updated or deleted.
     *
     * @param tableDefinition The TableDefinition entity to find columns for.
     * @return A list of ColumnDefinition entities.
     */
    List<ColumnDefinition> findByTableDefinition(TableDefinition tableDefinition);

    /**
     * Deletes all ColumnDefinition entities associated with a given TableDefinition.
     * This is used during the update process of a TableDefinition to replace old columns.
     *
     * @param tableDefinition The TableDefinition entity whose columns are to be deleted.
     */
    void deleteByTableDefinition(TableDefinition tableDefinition);
}