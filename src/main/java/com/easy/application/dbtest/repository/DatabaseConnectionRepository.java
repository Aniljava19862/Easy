package com.easy.application.dbtest.repository; // Adjust package

import com.easy.application.dbtest.data.DatabaseConnectionDetails; // Adjust import
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository // Marks this as a Spring Data JPA repository
public interface DatabaseConnectionRepository extends JpaRepository<DatabaseConnectionDetails, Long> {
    // You can add custom query methods here if needed, e.g.:
    Optional<DatabaseConnectionDetails> findByConnectionName(String connectionName);
}