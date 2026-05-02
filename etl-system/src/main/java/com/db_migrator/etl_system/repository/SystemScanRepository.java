package com.db_migrator.etl_system.repository;

import com.db_migrator.etl_system.model.entity.metadata.SystemScan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface SystemScanRepository extends JpaRepository<SystemScan, Long> {

    List<SystemScan> findByCreatedBy_Organization_Id(Long organizationId);

    Optional<SystemScan> findByIdAndCreatedBy_Organization_Id(Long id, Long organizationId);

    List<SystemScan> findBySourceConnector_IdAndCreatedBy_Organization_Id(Long connectorId, Long organizationId);

    boolean existsBySourceConnector_Id(Long connectorId);

    @Modifying
    @Query(value = "UPDATE system_scans SET status = :status, started_at = :startedAt WHERE id = :scanId", nativeQuery = true)
    void updateScanToRunning(@Param("scanId") Long scanId,
                             @Param("status") String status,
                             @Param("startedAt") Date startedAt);

    @Modifying
    @Query(value = "UPDATE system_scans SET status = :status, completed_at = :completedAt WHERE id = :scanId", nativeQuery = true)
    void updateScanToCompleted(@Param("scanId") Long scanId,
                               @Param("status") String status,
                               @Param("completedAt") Date completedAt);

    @Modifying
    @Query(value = "UPDATE system_scans SET status = :status, completed_at = :completedAt, error_message = :errorMessage WHERE id = :scanId", nativeQuery = true)
    void updateScanToFailed(@Param("scanId") Long scanId,
                            @Param("status") String status,
                            @Param("completedAt") Date completedAt,
                            @Param("errorMessage") String errorMessage);
}
