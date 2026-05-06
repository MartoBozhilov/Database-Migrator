package com.database_migrator.domain.transformation.repository;

import com.database_migrator.domain.transformation.model.TransformationTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransformationTableRepository extends JpaRepository<TransformationTable, Long> {
}
