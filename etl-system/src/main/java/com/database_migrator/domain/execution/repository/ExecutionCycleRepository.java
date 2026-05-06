package com.database_migrator.domain.execution.repository;

import com.database_migrator.domain.execution.model.Cycle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExecutionCycleRepository extends JpaRepository<Cycle, Long> {

    default boolean existsByTransformationModel_Id(Long transformationModelId) {
        return false; // For now, no cycles exist
    }
}
