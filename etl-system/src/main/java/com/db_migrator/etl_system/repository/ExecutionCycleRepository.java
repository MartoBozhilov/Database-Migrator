package com.db_migrator.etl_system.repository;

import com.db_migrator.etl_system.model.entity.execution.Cycle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExecutionCycleRepository extends JpaRepository<Cycle, Long> {

    default boolean existsByTransformationModel_Id(Long transformationModelId) {
        return false; // For now, no cycles exist
    }
}
