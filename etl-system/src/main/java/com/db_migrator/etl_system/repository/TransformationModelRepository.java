package com.db_migrator.etl_system.repository;

import com.db_migrator.etl_system.model.entity.transformation.TransformationModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransformationModelRepository extends JpaRepository<TransformationModel, Long> {

    List<TransformationModel> findByCreatedBy_Organization_Id(Long organizationId);

    Optional<TransformationModel> findByIdAndCreatedBy_Organization_Id(Long id, Long organizationId);

    boolean existsByNameAndCreatedBy_Organization_Id(String name, Long organizationId);

    boolean existsBySystemScan_Id(Long systemScanId);
}
