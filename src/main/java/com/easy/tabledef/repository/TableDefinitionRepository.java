package com.easy.tabledef.repository;


import com.easy.tabledef.model.TableDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TableDefinitionRepository extends JpaRepository<TableDefinition, Long> {

    Optional<TableDefinition> findByTableName(String tableName);

    Optional<TableDefinition> findByFinalTableName(String finalTableName);
}
