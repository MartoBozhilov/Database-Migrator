package com.db_migrator.etl_system.service;

import com.db_migrator.etl_system.dto.request.TransformationModelCreateRequest;
import com.db_migrator.etl_system.dto.request.TransformationModelUpdateRequest;
import com.db_migrator.etl_system.dto.response.TransformationModelDetailsResponse;
import com.db_migrator.etl_system.dto.response.TransformationModelResponse;
import com.db_migrator.etl_system.mapper.ResponseMapper;
import com.db_migrator.etl_system.model.entity.connector.Connector;
import com.db_migrator.etl_system.model.entity.metadata.ColumnMetadata;
import com.db_migrator.etl_system.model.entity.metadata.RelationMetadata;
import com.db_migrator.etl_system.model.entity.metadata.SystemScan;
import com.db_migrator.etl_system.model.entity.metadata.TableMetadata;
import com.db_migrator.etl_system.model.entity.transformation.TransformationColumn;
import com.db_migrator.etl_system.model.entity.transformation.TransformationModel;
import com.db_migrator.etl_system.model.entity.transformation.TransformationRelation;
import com.db_migrator.etl_system.model.entity.transformation.TransformationTable;
import com.db_migrator.etl_system.model.entity.user.User;
import com.db_migrator.etl_system.model.enums.ConnectorTypeEnum;
import com.db_migrator.etl_system.model.enums.ScanStatusEnum;
import com.db_migrator.etl_system.repository.ConnectorRepository;
import com.db_migrator.etl_system.repository.ExecutionCycleRepository;
import com.db_migrator.etl_system.repository.SystemScanRepository;
import com.db_migrator.etl_system.repository.TransformationModelRepository;
import com.db_migrator.etl_system.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransformationModelService {

    private final TransformationModelRepository modelRepository;
    private final SystemScanRepository scanRepository;
    private final ConnectorRepository connectorRepository;
    private final ExecutionCycleRepository cycleRepository;
    private final SecurityUtils securityUtils;
    private final ResponseMapper responseMapper;

    @Transactional
    public TransformationModelDetailsResponse create(TransformationModelCreateRequest request) {
        Long orgId = securityUtils.getCurrentOrganizationId();
        User currentUser = securityUtils.getCurrentUser();

        // Check duplicate name within organization
        if (modelRepository.existsByNameAndCreatedBy_Organization_Id(request.getName(), orgId)) {
            throw new RuntimeException("Transformation model with name '" + request.getName() + "' already exists in your organization");
        }

        // Verify SystemScan exists, belongs to org, and is COMPLETED
        SystemScan scan = scanRepository.findByIdAndCreatedBy_Organization_Id(request.getSystemScanId(), orgId)
                .orElseThrow(() -> new RuntimeException("System scan not found"));

        if (scan.getStatus() != ScanStatusEnum.COMPLETED) {
            throw new RuntimeException("System scan must be in COMPLETED status. Current status: " + scan.getStatus());
        }

        // Verify target connector exists, belongs to org, and is TARGET type
        Connector targetConnector = connectorRepository.findByIdAndCreatedBy_Organization_Id(request.getTargetConnectorId(), orgId)
                .orElseThrow(() -> new RuntimeException("Target connector not found"));

        if (targetConnector.getConnectorType() != ConnectorTypeEnum.TARGET) {
            throw new RuntimeException("Connector must be of type TARGET. Current type: " + targetConnector.getConnectorType());
        }

        TransformationModel model = buildTransformationModel(request.getName(), currentUser, scan, targetConnector);

        model = modelRepository.save(model);
        return responseMapper.toTransformationModelDetailsResponse(model);
    }

    @Transactional(readOnly = true)
    public List<TransformationModelResponse> findAll() {
        Long orgId = securityUtils.getCurrentOrganizationId();
        return modelRepository.findByCreatedBy_Organization_Id(orgId).stream()
                .map(responseMapper::toTransformationModelResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TransformationModelResponse findById(Long id) {
        Long orgId = securityUtils.getCurrentOrganizationId();
        TransformationModel model = modelRepository.findByIdAndCreatedBy_Organization_Id(id, orgId)
                .orElseThrow(() -> new RuntimeException("Transformation model not found"));
        return responseMapper.toTransformationModelResponse(model);
    }

    @Transactional(readOnly = true)
    public TransformationModelDetailsResponse getDetails(Long id) {
        Long orgId = securityUtils.getCurrentOrganizationId();
        TransformationModel model = modelRepository.findByIdAndCreatedBy_Organization_Id(id, orgId)
                .orElseThrow(() -> new RuntimeException("Transformation model not found"));
        return responseMapper.toTransformationModelDetailsResponse(model);
    }

    /**
     * UPDATE - Update transformation model name only
     * Note: SystemScan and TargetConnector cannot be changed after creation
     */
    @Transactional
    public TransformationModelDetailsResponse update(Long id, TransformationModelUpdateRequest request) {
        Long orgId = securityUtils.getCurrentOrganizationId();

        TransformationModel model = modelRepository.findByIdAndCreatedBy_Organization_Id(id, orgId)
                .orElseThrow(() -> new RuntimeException("Transformation model not found"));

        // Check name uniqueness (only if name is different)
        if (!request.getName().equals(model.getName())) {
            if (modelRepository.existsByNameAndCreatedBy_Organization_Id(request.getName(), orgId)) {
                throw new RuntimeException("Transformation model with name '" + request.getName() + "' already exists in your organization");
            }
            model.setName(request.getName());
        }

        model = modelRepository.save(model);
        return responseMapper.toTransformationModelDetailsResponse(model);
    }

    @Transactional
    public void delete(Long id) {
        Long orgId = securityUtils.getCurrentOrganizationId();

        TransformationModel model = modelRepository.findByIdAndCreatedBy_Organization_Id(id, orgId)
                .orElseThrow(() -> new RuntimeException("Transformation model not found"));

        if (cycleRepository.existsByTransformationModel_Id(id)) {
            throw new RuntimeException("Cannot delete transformation model - it is used by execution cycles");
        }

        modelRepository.delete(model);
    }

    private TransformationModel buildTransformationModel(String name, User currentUser, SystemScan scan, Connector targetConnector) {
        TransformationModel model = new TransformationModel();
        model.setName(name);
        model.setCreatedBy(currentUser);
        model.setCreatedAt(new Date());
        model.setSystemScan(scan);
        model.setTargetConnector(targetConnector);

        for (TableMetadata tableMetadata : scan.getTableMetadataList()) {
            TransformationTable transformationTable = buildTransformationTable(model, tableMetadata);
            model.getTransformationTables().add(transformationTable);
        }

        for (RelationMetadata relationMetadata : scan.getRelationMetadataList()) {
            TransformationRelation transformationRelation = buildTransformationRelation(model, relationMetadata);
            model.getTransformationRelations().add(transformationRelation);
        }

        return model;
    }

    private TransformationTable buildTransformationTable(TransformationModel model, TableMetadata tableMetadata) {
        TransformationTable transformationTable = new TransformationTable();
        transformationTable.setTransformationModel(model);
        transformationTable.setSourceTableMetadata(tableMetadata);

        for (ColumnMetadata columnMetadata : tableMetadata.getColumnMetadataList()) {
            TransformationColumn transformationColumn = new TransformationColumn();
            transformationColumn.setTransformationTable(transformationTable);
            transformationColumn.setSourceColumnMetadata(columnMetadata);
            transformationTable.getColumns().add(transformationColumn);
        }

        return transformationTable;
    }

    private TransformationRelation buildTransformationRelation(TransformationModel model, RelationMetadata relationMetadata) {
        TransformationRelation transformationRelation = new TransformationRelation();
        transformationRelation.setTransformationModel(model);
        transformationRelation.setSourceRelationMetadata(relationMetadata);
        transformationRelation.setIsDeleted(false);
        transformationRelation.setForeignTable(relationMetadata.getForeignTable());
        transformationRelation.setForeignColumn(relationMetadata.getForeignColumn());
        transformationRelation.setPrimaryTable(relationMetadata.getPrimaryTable());
        transformationRelation.setPrimaryColumn(relationMetadata.getPrimaryColumn());
        return transformationRelation;
    }
}
