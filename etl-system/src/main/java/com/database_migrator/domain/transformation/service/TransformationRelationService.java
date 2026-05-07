package com.database_migrator.domain.transformation.service;

import com.database_migrator.domain.transformation.dto.RelationAddRequest;
import com.database_migrator.domain.transformation.dto.TransformationModelDetailsResponse;
import com.database_migrator.domain.common.mapper.ResponseMapper;
import com.database_migrator.domain.transformation.model.TransformationModel;
import com.database_migrator.domain.transformation.model.TransformationRelation;
import com.database_migrator.domain.transformation.repository.TransformationModelRepository;
import com.database_migrator.domain.transformation.repository.TransformationRelationRepository;
import com.database_migrator.domain.common.util.SecurityUtils;
import com.database_migrator.domain.common.exception.ResourceNotFoundException;
import com.database_migrator.domain.common.exception.BusinessRuleException;
import com.database_migrator.domain.common.exception.ValidationException;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.database_migrator.domain.common.util.TransformationUtils.getEffectiveTableName;
import static com.database_migrator.domain.common.util.TransformationUtils.getEffectiveColumnName;
import static com.database_migrator.domain.common.util.TransformationUtils.isTableDeleted;
import static com.database_migrator.domain.common.util.TransformationUtils.isColumnDeleted;
import static com.database_migrator.domain.common.util.TransformationUtils.validateModelNotConfirmed;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransformationRelationService {

    private final TransformationModelRepository modelRepository;
    private final TransformationRelationRepository relationRepository;
    private final SecurityUtils securityUtils;
    private final ResponseMapper responseMapper;
    private final EntityManager entityManager;

    @Transactional
    public TransformationModelDetailsResponse addRelation(Long modelId, RelationAddRequest request) {
        Long orgId = securityUtils.getCurrentOrganizationId();

        TransformationModel model = modelRepository.findByIdAndCreatedBy_Organization_Id(modelId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("TransformationModel", modelId));

        validateModelNotConfirmed(model);

        validateTableExists(model, request.getForeignTable(), "Foreign table");
        validateTableExists(model, request.getPrimaryTable(), "Primary table");

        validateColumnExists(model, request.getForeignTable(), request.getForeignColumn(), "Foreign column");
        validateColumnExists(model, request.getPrimaryTable(), request.getPrimaryColumn(), "Primary column");

        // Check if relation already exists
        Optional<TransformationRelation> existing = model.getTransformationRelations().stream()
                .filter(r -> !r.getIsDeleted())
                .filter(r -> r.getForeignTable().equalsIgnoreCase(request.getForeignTable())
                        && r.getForeignColumn().equalsIgnoreCase(request.getForeignColumn())
                        && r.getPrimaryTable().equalsIgnoreCase(request.getPrimaryTable())
                        && r.getPrimaryColumn().equalsIgnoreCase(request.getPrimaryColumn()))
                .findFirst();

        if (existing.isPresent()) {
            throw new ValidationException("Relation already exists between " + request.getForeignTable() + "." +
                    request.getForeignColumn() + " and " + request.getPrimaryTable() + "." + request.getPrimaryColumn(),
                    List.of("Duplicate relation"));
        }

        relationRepository.save(createRelation(model, request));

        return getModelDetailsResponse(modelId, orgId);
    }

    @Transactional
    public TransformationModelDetailsResponse deleteRelation(Long modelId, Long relationId) {
        Long orgId = securityUtils.getCurrentOrganizationId();

        TransformationModel model = modelRepository.findByIdAndCreatedBy_Organization_Id(modelId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("TransformationModel", modelId));

        validateModelNotConfirmed(model);

        TransformationRelation relation = relationRepository.findById(relationId)
                .orElseThrow(() -> new ResourceNotFoundException("TransformationRelation", relationId));

        if (!relation.getTransformationModel().getId().equals(modelId)) {
            throw new BusinessRuleException("Relation does not belong to this transformation model",
                    "RELATION_MODEL_MISMATCH");
        }

        // Soft delete relation
        relation.setIsDeleted(true);
        relationRepository.save(relation);

        return getModelDetailsResponse(modelId, orgId);
    }

    private TransformationRelation createRelation(TransformationModel model, RelationAddRequest request) {
        TransformationRelation relation = new TransformationRelation();
        relation.setTransformationModel(model);
        relation.setSourceRelationMetadata(null);
        relation.setIsDeleted(false);
        relation.setForeignTable(request.getForeignTable());
        relation.setForeignColumn(request.getForeignColumn());
        relation.setPrimaryTable(request.getPrimaryTable());
        relation.setPrimaryColumn(request.getPrimaryColumn());

        return relation;
    }

    private void validateTableExists(TransformationModel model, String tableName, String tableType) {
        boolean exists = model.getTransformationTables().stream()
                .anyMatch(table -> Objects.requireNonNull(getEffectiveTableName(table)).equalsIgnoreCase(tableName)
                        && !isTableDeleted(table));

        if (!exists) {
            throw new ValidationException(tableType + " '" + tableName + "' not found in transformation model",
                    List.of("Table not found: " + tableName));
        }
    }

    private void validateColumnExists(TransformationModel model, String tableName, String columnName, String columnType) {
        var table = model.getTransformationTables().stream()
                .filter(t -> Objects.requireNonNull(getEffectiveTableName(t)).equalsIgnoreCase(tableName))
                .filter(t -> !isTableDeleted(t))
                .findFirst()
                .orElseThrow(() -> new ValidationException("Table '" + tableName + "' not found",
                        List.of("Table not found")));

        // Check if column exists in the table
        boolean columnExists = table.getColumns().stream()
                .anyMatch(column -> Objects.requireNonNull(getEffectiveColumnName(column)).equalsIgnoreCase(columnName)
                        && !isColumnDeleted(column));

        if (!columnExists) {
            throw new ValidationException(columnType + " '" + columnName + "' not found in table '" + tableName + "'",
                    List.of("Column not found: " + columnName + " in table " + tableName));
        }
    }

    private TransformationModelDetailsResponse getModelDetailsResponse(Long modelId, Long orgId) {
        entityManager.flush();
        entityManager.clear();
        TransformationModel model = modelRepository.findByIdAndCreatedBy_Organization_Id(modelId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("TransformationModel", modelId));
        return responseMapper.toTransformationModelDetailsResponse(model);
    }
}
