package com.database_migrator.domain.transformation.service;

import static com.database_migrator.domain.common.util.TransformationUtils.validateModelNotConfirmed;

import com.database_migrator.domain.transformation.dto.TransformationModelCreateRequest;
import com.database_migrator.domain.transformation.dto.TransformationModelUpdateRequest;
import com.database_migrator.domain.transformation.dto.TransformationModelDetailsResponse;
import com.database_migrator.domain.transformation.dto.TransformationModelResponse;
import com.database_migrator.domain.common.mapper.ResponseMapper;
import com.database_migrator.domain.connector.model.Connector;
import com.database_migrator.domain.scan.model.ColumnMetadata;
import com.database_migrator.domain.scan.model.RelationMetadata;
import com.database_migrator.domain.scan.model.SystemScan;
import com.database_migrator.domain.scan.model.TableMetadata;
import com.database_migrator.domain.transformation.model.TransformationColumn;
import com.database_migrator.domain.transformation.model.TransformationModel;
import com.database_migrator.domain.transformation.model.TransformationRelation;
import com.database_migrator.domain.transformation.model.TransformationTable;
import com.database_migrator.domain.auth.model.User;
import com.database_migrator.domain.connector.model.ConnectorTypeEnum;
import com.database_migrator.domain.scan.model.ScanStatusEnum;
import com.database_migrator.domain.connector.repository.ConnectorRepository;
import com.database_migrator.domain.execution.repository.ExecutionCycleRepository;
import com.database_migrator.domain.scan.repository.SystemScanRepository;
import com.database_migrator.domain.transformation.repository.TransformationModelRepository;
import com.database_migrator.domain.common.util.SecurityUtils;
import com.database_migrator.domain.execution.service.DAGBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private final TypeResolutionService typeResolutionService;
    private final DAGBuilder dagBuilder;

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

        // Auto-resolve column types for cross-database migrations
        typeResolutionService.autoResolveAllColumnTypes(model);
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

    @Transactional
    public TransformationModelDetailsResponse update(Long id, TransformationModelUpdateRequest request) {
        Long orgId = securityUtils.getCurrentOrganizationId();

        TransformationModel model = modelRepository.findByIdAndCreatedBy_Organization_Id(id, orgId)
                .orElseThrow(() -> new RuntimeException("Transformation model not found"));

        validateModelNotConfirmed(model);

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

    @Transactional
    public TransformationModelDetailsResponse confirmModel(Long id) {
        Long orgId = securityUtils.getCurrentOrganizationId();

        TransformationModel model = modelRepository.findByIdAndCreatedBy_Organization_Id(id, orgId)
                .orElseThrow(() -> new RuntimeException("Transformation model not found"));

        if (model.getIsConfirmed()) {
            throw new RuntimeException("Transformation model is already confirmed");
        }

        // Validate model (DAG check)
        List<String> validationErrors = validateModelForConfirmation(model);

        if (!validationErrors.isEmpty()) {
            String errorMessage = "Transformation model validation failed:\n" + String.join("\n", validationErrors);
            throw new RuntimeException(errorMessage);
        }

        model.setIsConfirmed(true);
        modelRepository.save(model);

        return responseMapper.toTransformationModelDetailsResponse(model);
    }

    private List<String> validateModelForConfirmation(TransformationModel model) {
        // Build dependency graph
        Map<String, Set<String>> graph = dagBuilder.buildDependencyGraph(model);

        // Find cycle paths
        List<String> cyclePaths = dagBuilder.findCyclePaths(graph);

        return cyclePaths.stream()
                .map(cyclePath -> "Cycle detected in relationships: " + cyclePath)
                .collect(Collectors.toList());
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
