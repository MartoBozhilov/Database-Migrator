package com.database_migrator.domain.transformation.repository;

import com.database_migrator.domain.transformation.model.TableTransformationAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TableTransformationAssignmentRepository extends JpaRepository<TableTransformationAssignment, Long> {
}
