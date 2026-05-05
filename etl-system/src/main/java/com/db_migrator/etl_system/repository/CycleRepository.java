package com.db_migrator.etl_system.repository;

import com.db_migrator.etl_system.model.entity.execution.Cycle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CycleRepository extends JpaRepository<Cycle, Long> {

    List<Cycle> findByCreatedBy_Organization_Id(Long organizationId);

    Optional<Cycle> findByIdAndCreatedBy_Organization_Id(Long id, Long organizationId);

    boolean existsByNameAndCreatedBy_Organization_Id(String name, Long organizationId);
}
