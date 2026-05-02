package com.db_migrator.etl_system.repository;

import com.db_migrator.etl_system.model.entity.connector.Connector;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConnectorRepository extends JpaRepository<Connector, Long> {

    List<Connector> findByCreatedBy_Organization_Id(Long organizationId);

    Optional<Connector> findByIdAndCreatedBy_Organization_Id(Long id, Long organizationId);

    boolean existsByNameAndCreatedBy_Organization_Id(String name, Long organizationId);
}
