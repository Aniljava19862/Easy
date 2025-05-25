package com.easy.tabledef.repository;


import com.easy.tabledef.model.TableMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TableDefinitionRepository extends JpaRepository<TableMetadata, Long> {
    Optional<TableMetadata> findByTableName(String tableName);
}
