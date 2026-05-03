package com.db_migrator.etl_system.repository;

import com.db_migrator.etl_system.model.entity.transformation.TransformationTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransformationTableRepository extends JpaRepository<TransformationTable, Long> {
}
