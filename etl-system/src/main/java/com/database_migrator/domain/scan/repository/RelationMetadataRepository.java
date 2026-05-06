package com.database_migrator.domain.scan.repository;

import com.database_migrator.domain.scan.model.RelationMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RelationMetadataRepository extends JpaRepository<RelationMetadata, Long> {
}
