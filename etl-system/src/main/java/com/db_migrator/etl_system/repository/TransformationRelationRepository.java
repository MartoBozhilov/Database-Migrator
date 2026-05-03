package com.db_migrator.etl_system.repository;

import com.db_migrator.etl_system.model.entity.transformation.TransformationRelation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransformationRelationRepository extends JpaRepository<TransformationRelation, Long> {

    /**
     * Find all relations where table appears in either side
     */
    @Query("SELECT tr FROM TransformationRelation tr WHERE tr.transformationModel.id = :modelId " +
           "AND (tr.foreignTable = :tableName OR tr.primaryTable = :tableName) AND tr.isDeleted = false")
    List<TransformationRelation> findActiveRelationsByTable(@Param("modelId") Long modelId,
                                                             @Param("tableName") String tableName);
}
