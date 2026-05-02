package com.db_migrator.etl_system.repository;

import com.db_migrator.etl_system.model.entity.metadata.TableMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TableMetadataRepository extends JpaRepository<TableMetadata, Long> {
}
