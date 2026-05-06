package com.database_migrator.domain.transformation.service;

import com.database_migrator.domain.transformation.dto.RelationAddRequest;
import com.database_migrator.domain.transformation.dto.TransformationModelDetailsResponse;
import com.database_migrator.domain.common.mapper.ResponseMapper;
import com.database_migrator.domain.transformation.model.TransformationModel;
import com.database_migrator.domain.transformation.model.TransformationRelation;
import com.database_migrator.domain.transformation.repository.TransformationModelRepository;
import com.database_migrator.domain.transformation.repository.TransformationRelationRepository;
import com.database_migrator.domain.common.util.SecurityUtils;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

import static com.database_migrator.domain.common.util.TransformationUtils.getEffectiveTableName;
import static com.database_migrator.domain.common.util.TransformationUtils.isTableDeleted;
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
                .orElseThrow(() -> new RuntimeException("Transformation model not found"));

        validateModelNotConfirmed(model);

        // Validate tables exist in transformation model
        validateTableExists(model, request.getForeignTable(), "Foreign table");
        validateTableExists(model, request.getPrimaryTable(), "Primary table");

        // Check if relation already exists
        Optional<TransformationRelation> existing = model.getTransformationRelations().stream()
                .filter(r -> !r.getIsDeleted())
                .filter(r -> r.getForeignTable().equalsIgnoreCase(request.getForeignTable())
                        && r.getForeignColumn().equalsIgnoreCase(request.getForeignColumn())
                        && r.getPrimaryTable().equalsIgnoreCase(request.getPrimaryTable())
                        && r.getPrimaryColumn().equalsIgnoreCase(request.getPrimaryColumn()))
                .findFirst();

        if (existing.isPresent()) {
            throw new RuntimeException("Relation already exists between " + request.getForeignTable() + "." +
                    request.getForeignColumn() + " and " + request.getPrimaryTable() + "." + request.getPrimaryColumn());
        }

        createRelation(model, request);

        return getModelDetailsResponse(modelId, orgId);
    }

    @Transactional
    public TransformationModelDetailsResponse deleteRelation(Long modelId, Long relationId) {
        Long orgId = securityUtils.getCurrentOrganizationId();

        TransformationModel model = modelRepository.findByIdAndCreatedBy_Organization_Id(modelId, orgId)
                .orElseThrow(() -> new RuntimeException("Transformation model not found"));

        validateModelNotConfirmed(model);

        TransformationRelation relation = relationRepository.findById(relationId)
                .orElseThrow(() -> new RuntimeException("Relation not found"));

        if (!relation.getTransformationModel().getId().equals(modelId)) {
            throw new RuntimeException("Relation does not belong to this transformation model");
        }

        // Soft delete relation
        relation.setIsDeleted(true);
        relationRepository.save(relation);

        return getModelDetailsResponse(modelId, orgId);
    }

    private TransformationRelation createRelation(TransformationModel model, RelationAddRequest request) {
        TransformationRelation relation = new TransformationRelation();
        relation.setTransformationModel(model);
        relation.setSourceRelationMetadata(null); // User-created relation
        relation.setIsDeleted(false);
        relation.setForeignTable(request.getForeignTable());
        relation.setForeignColumn(request.getForeignColumn());
        relation.setPrimaryTable(request.getPrimaryTable());
        relation.setPrimaryColumn(request.getPrimaryColumn());

        return relationRepository.save(relation);
    }

    private void validateTableExists(TransformationModel model, String tableName, String tableType) {
        boolean exists = model.getTransformationTables().stream()
                .anyMatch(table -> Objects.requireNonNull(getEffectiveTableName(table)).equalsIgnoreCase(tableName)
                        && !isTableDeleted(table));

        if (!exists) {
            throw new RuntimeException(tableType + " '" + tableName + "' not found in transformation model");
        }
    }

    private TransformationModelDetailsResponse getModelDetailsResponse(Long modelId, Long orgId) {
        entityManager.flush();
        entityManager.clear();
        TransformationModel model = modelRepository.findByIdAndCreatedBy_Organization_Id(modelId, orgId)
                .orElseThrow(() -> new RuntimeException("Transformation model not found"));
        return responseMapper.toTransformationModelDetailsResponse(model);
    }
}
