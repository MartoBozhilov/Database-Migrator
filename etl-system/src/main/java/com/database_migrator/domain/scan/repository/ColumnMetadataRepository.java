package com.database_migrator.domain.scan.repository;

import com.database_migrator.domain.scan.model.ColumnMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ColumnMetadataRepository extends JpaRepository<ColumnMetadata, Long> {
}
