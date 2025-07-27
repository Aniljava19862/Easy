package com.easy.tabledef.repository;

import com.easy.tabledef.model.TableDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TableDefinitionRepository extends JpaRepository<TableDefinition, String> {

    /**
     * Finds a TableDefinition by its logical table name and the associated project configuration ID.
     * This is crucial for ensuring unique table names within a project and for retrieving specific table schemas.
     *
     * @param tableName The logical name of the table.
     * @param projectConfigIdRef The ID of the project configuration.
     * @return An Optional containing the TableDefinition if found, otherwise empty.
     */
    Optional<TableDefinition> findByTableNameAndProjectConfigIdRef(String tableName, String projectConfigIdRef);

    /**
     * Finds all TableDefinitions associated with a specific project configuration ID.
     *
     * @param projectConfigIdRef The ID of the project configuration.
     * @return A list of TableDefinition entities.
     */
    List<TableDefinition> findByProjectConfigIdRef(String projectConfigIdRef);
}