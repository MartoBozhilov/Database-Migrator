package com.database_migrator.domain.execution.repository;

import com.database_migrator.domain.execution.model.Cycle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CycleRepository extends JpaRepository<Cycle, Long> {

    List<Cycle> findByCreatedBy_Organization_Id(Long organizationId);

    Optional<Cycle> findByIdAndCreatedBy_Organization_Id(Long id, Long organizationId);

    @Query("SELECT DISTINCT c FROM Cycle c " +
           "LEFT JOIN FETCH c.tasks " +
           "WHERE c.id = :id AND c.createdBy.organization.id = :orgId")
    Optional<Cycle> findByIdWithTasksAndLogs(@Param("id") Long id, @Param("orgId") Long organizationId);

    boolean existsByNameAndCreatedBy_Organization_Id(String name, Long organizationId);
}
