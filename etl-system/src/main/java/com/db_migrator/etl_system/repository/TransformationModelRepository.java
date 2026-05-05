package com.db_migrator.etl_system.repository;

import com.db_migrator.etl_system.model.entity.transformation.TransformationModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransformationModelRepository extends JpaRepository<TransformationModel, Long> {

    List<TransformationModel> findByCreatedBy_Organization_Id(Long organizationId);

    Optional<TransformationModel> findByIdAndCreatedBy_Organization_Id(Long id, Long organizationId);

    boolean existsByNameAndCreatedBy_Organization_Id(String name, Long organizationId);

    boolean existsBySystemScan_Id(Long systemScanId);

    @Query("SELECT DISTINCT tm FROM TransformationModel tm " +
           "LEFT JOIN FETCH tm.systemScan ss " +
           "LEFT JOIN FETCH ss.sourceConnector sc " +
           "LEFT JOIN FETCH tm.targetConnector tc " +
           "LEFT JOIN FETCH tm.transformationRelations tr " +
           "WHERE tm.id = :id")
    Optional<TransformationModel> findByIdWithAssociations(@Param("id") Long id);
}
