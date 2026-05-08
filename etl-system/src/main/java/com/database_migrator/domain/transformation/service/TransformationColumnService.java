package com.database_migrator.domain.transformation.service;

import com.database_migrator.config.migration.loaders.TypeMappingLoader;
import com.database_migrator.domain.transformation.dto.ColumnAddRequest;
import com.database_migrator.domain.transformation.dto.ColumnChangeTypeRequest;
import com.database_migrator.domain.transformation.dto.ColumnRenameRequest;
import com.database_migrator.domain.transformation.dto.TransformationModelDetailsResponse;
import com.database_migrator.domain.common.mapper.ResponseMapper;
import com.database_migrator.domain.transformation.model.ColumnTransformationAssignment;
import com.database_migrator.domain.transformation.model.TransformationColumn;
import com.database_migrator.domain.transformation.model.TransformationModel;
import com.database_migrator.domain.transformation.model.TransformationRelation;
import com.database_migrator.domain.transformation.model.TransformationTable;
import com.database_migrator.domain.transformation.model.ColumnTransformationType;
import com.database_migrator.domain.connector.model.DatabaseTypeEnum;
import com.database_migrator.domain.transformation.repository.ColumnTransformationAssignmentRepository;
import com.database_migrator.domain.transformation.repository.TransformationColumnRepository;
import com.database_migrator.domain.transformation.repository.TransformationModelRepository;
import com.database_migrator.domain.transformation.repository.TransformationRelationRepository;
import com.database_migrator.domain.transformation.repository.TransformationTableRepository;
import com.database_migrator.domain.common.util.SecurityUtils;
import com.database_migrator.domain.common.util.TransformationUtils;
import com.database_migrator.domain.common.exception.ResourceNotFoundException;
import com.database_migrator.domain.common.exception.BusinessRuleException;
import com.database_migrator.domain.common.exception.ValidationException;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.database_migrator.domain.common.util.TransformationUtils.getEffectiveColumnName;
import static com.database_migrator.domain.common.util.TransformationUtils.getEffectiveTableName;
import static com.database_migrator.domain.common.util.TransformationUtils.isColumnDeleted;
import static com.database_migrator.domain.common.util.TransformationUtils.validateModelNotConfirmed;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransformationColumnService {

    private final TransformationModelRepository modelRepository;
    private final TransformationTableRepository tableRepository;
    private final TransformationColumnRepository columnRepository;
    private final ColumnTransformationAssignmentRepository assignmentRepository;
    private final TransformationRelationRepository relationRepository;
    private final SqlIdentifierValidator sqlIdentifierValidator;
    private final SecurityUtils securityUtils;
    private final ResponseMapper responseMapper;
    private final EntityManager entityManager;
    private final TypeResolutionService typeResolutionService;
    private final TypeMappingLoader typeMappingLoader;

    @Transactional
    public TransformationModelDetailsResponse renameColumn(Long modelId, Long tableId, Long columnId,
                                                           ColumnRenameRequest request) {
        Long orgId = securityUtils.getCurrentOrganizationId();

        TransformationModel model = modelRepository.findByIdAndCreatedBy_Organization_Id(modelId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("TransformationModel", modelId));

        validateModelNotConfirmed(model);

        TransformationTable table = getTableAndValidateOwnership(tableId, modelId);
        TransformationColumn column = getColumnAndValidateOwnership(columnId, tableId);

        validateColumnName(request.getNewName(), model);

        // Check for duplicate column name within the same table
        checkDuplicateColumnName(request.getNewName(), table, columnId);

        // Get old column name before renaming
        String oldColumnName = getEffectiveColumnName(column);
        String tableName = getEffectiveTableName(table);

        ColumnTransformationAssignment assignment = findOrCreateAssignment(column, ColumnTransformationType.RENAME_COLUMN);
        assignment.setNewName(request.getNewName());
        assignmentRepository.save(assignment);

        // Update all foreign key relationships that reference this column
        updateRelationsForRenamedColumn(modelId, tableName, oldColumnName, request.getNewName());

        return getModelDetailsResponse(modelId, orgId);
    }

    @Transactional
    public TransformationModelDetailsResponse changeColumnType(Long modelId, Long tableId, Long columnId,
                                                               ColumnChangeTypeRequest request) {
        Long orgId = securityUtils.getCurrentOrganizationId();

        TransformationModel model = modelRepository.findByIdAndCreatedBy_Organization_Id(modelId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("TransformationModel", modelId));

        validateModelNotConfirmed(model);

        TransformationColumn column = getColumnAndValidateOwnership(columnId, tableId);

        DatabaseTypeEnum targetDb = model.getTargetConnector().getDatabaseType();
        String sourceType;
        DatabaseTypeEnum sourceDb;

        // Handle both scanned columns and user-added columns
        if (column.getSourceColumnMetadata() != null) {
            sourceDb = model.getSystemScan().getSourceConnector().getDatabaseType();
            sourceType = column.getSourceColumnMetadata().getDataType();
        } else {
            sourceDb = targetDb;
            sourceType = column.getAssignments().stream()
                    .filter(a -> a.getTransformationType() == ColumnTransformationType.ADD_COLUMN)
                    .findFirst()
                    .map(ColumnTransformationAssignment::getDataType)
                    .orElseThrow(() -> new BusinessRuleException(
                            "Cannot find ADD_COLUMN assignment for this column",
                            "MISSING_ADD_COLUMN_ASSIGNMENT"));
        }

        boolean isValid = typeMappingLoader.isValidTypeConversion(sourceType, sourceDb, request.getTargetDataType(), targetDb);
        if (!isValid) {
            throw new ValidationException(
                    String.format("Invalid type conversion: %s %s -> %s %s. Check allowed type mappings.",
                            sourceDb, sourceType, targetDb, request.getTargetDataType()),
                    List.of("Invalid type conversion from " + sourceType + " to " + request.getTargetDataType())
            );
        }

        ColumnTransformationAssignment assignment = findOrCreateAssignment(column, ColumnTransformationType.CHANGE_TYPE);
        assignment.setTargetDataType(request.getTargetDataType());
        assignmentRepository.save(assignment);

        // Update resolved target type
        column.setResolvedTargetType(request.getTargetDataType());
        columnRepository.save(column);

        String columnName = TransformationUtils.getEffectiveColumnName(column);

        TransformationModelDetailsResponse response = getModelDetailsResponse(modelId, orgId);
        addTypeConversionWarningIfNeeded(response, sourceType, sourceDb, request.getTargetDataType(), targetDb, columnName);

        return response;
    }

    @Transactional
    public TransformationModelDetailsResponse addColumn(Long modelId, Long tableId, ColumnAddRequest request) {
        Long orgId = securityUtils.getCurrentOrganizationId();

        TransformationModel model = modelRepository.findByIdAndCreatedBy_Organization_Id(modelId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("TransformationModel", modelId));

        validateModelNotConfirmed(model);

        TransformationTable table = getTableAndValidateOwnership(tableId, modelId);

        validateColumnName(request.getColumnName(), model);

        // Validate data type is valid for target database
        boolean isValidType = typeMappingLoader.isValidTargetType(
                request.getDataType(),
                model.getTargetConnector().getDatabaseType()
        );

        if (!isValidType) {
            throw new ValidationException(
                    String.format("Invalid data type '%s' for target database %s. Please use a valid type for this database.",
                            request.getDataType(),
                            model.getTargetConnector().getDatabaseType()),
                    List.of("Invalid data type: " + request.getDataType())
            );
        }

        // Check for duplicate column name
        checkDuplicateColumnName(request.getColumnName(), table, null);

        // Create new column
        TransformationColumn column = new TransformationColumn();
        column.setTransformationTable(table);
        column.setSourceColumnMetadata(null);

        // Resolve target type
        String resolvedType = typeResolutionService.resolveAddColumnType(request.getDataType());
        column.setResolvedTargetType(resolvedType);

        column = columnRepository.save(column);

        assignmentRepository.save(createAddColumnAssignment(column, request));

        return getModelDetailsResponse(modelId, orgId);
    }

    @Transactional
    public TransformationModelDetailsResponse deleteColumn(Long modelId, Long tableId, Long columnId) {
        Long orgId = securityUtils.getCurrentOrganizationId();

        TransformationModel model = modelRepository.findByIdAndCreatedBy_Organization_Id(modelId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("TransformationModel", modelId));

        validateModelNotConfirmed(model);

        TransformationTable table = getTableAndValidateOwnership(tableId, modelId);
        TransformationColumn column = getColumnAndValidateOwnership(columnId, tableId);

        // Validate column deletion
        validateColumnDeletion(column, table, model);

        ColumnTransformationAssignment assignment = findOrCreateAssignment(column, ColumnTransformationType.DELETE_COLUMN);
        assignmentRepository.save(assignment);

        return getModelDetailsResponse(modelId, orgId);
    }

    private void validateColumnDeletion(TransformationColumn column, TransformationTable table, TransformationModel model) {
        String tableName = getEffectiveTableName(table);
        String columnName = getEffectiveColumnName(column);

        validateNotPrimaryKeyInForeignKey(columnName, tableName, model);
        validateNotUsedInForeignKey(columnName, tableName, model);
    }

    private void validateNotPrimaryKeyInForeignKey(String columnName, String tableName, TransformationModel model) {
        if (columnName == null || tableName == null) {
            return;
        }

        // Check if column looks like a primary key (id or ends with _id)
        if (!columnName.equalsIgnoreCase("id") && !columnName.toLowerCase().endsWith("_id")) {
            return;
        }

        boolean isReferencedAsPrimaryKey = model.getTransformationRelations().stream()
                .filter(rel -> !rel.getIsDeleted())
                .anyMatch(rel -> rel.getPrimaryTable().equalsIgnoreCase(tableName)
                        && rel.getPrimaryColumn().equalsIgnoreCase(columnName));

        if (isReferencedAsPrimaryKey) {
            throw new BusinessRuleException(
                    String.format("Cannot delete column '%s' because it is referenced as a primary key in foreign key relationships. " +
                            "Please delete the foreign key relationships first.", columnName),
                    "COLUMN_REFERENCED_AS_PRIMARY_KEY"
            );
        }
    }

    private void validateNotUsedInForeignKey(String columnName, String tableName, TransformationModel model) {
        if (columnName == null || tableName == null) {
            return;
        }

        boolean isUsedInForeignKey = model.getTransformationRelations().stream()
                .filter(rel -> !rel.getIsDeleted())
                .anyMatch(rel -> rel.getForeignTable().equalsIgnoreCase(tableName)
                        && rel.getForeignColumn().equalsIgnoreCase(columnName));

        if (isUsedInForeignKey) {
            throw new BusinessRuleException(
                    String.format("Cannot delete column '%s' because it is part of a foreign key relationship. " +
                            "Please delete the foreign key relationship first.", columnName),
                    "COLUMN_USED_IN_FOREIGN_KEY"
            );
        }
    }

    private void validateColumnName(String columnName, TransformationModel model) {
        if (columnName == null || columnName.trim().isEmpty()) {
            throw new ValidationException("Column name is required", List.of("Column name cannot be null or empty"));
        }
        sqlIdentifierValidator.validateIdentifier(columnName, model.getTargetConnector().getDatabaseType());
    }

    private TransformationTable getTableAndValidateOwnership(Long tableId, Long modelId) {
        TransformationTable table = tableRepository.findById(tableId)
                .orElseThrow(() -> new ResourceNotFoundException("TransformationTable", tableId));

        if (!table.getTransformationModel().getId().equals(modelId)) {
            throw new BusinessRuleException("Table does not belong to this transformation model",
                    "TABLE_MODEL_MISMATCH");
        }

        return table;
    }

    private TransformationColumn getColumnAndValidateOwnership(Long columnId, Long tableId) {
        TransformationColumn column = columnRepository.findById(columnId)
                .orElseThrow(() -> new ResourceNotFoundException("TransformationColumn", columnId));

        if (!column.getTransformationTable().getId().equals(tableId)) {
            throw new BusinessRuleException("Column does not belong to this table",
                    "COLUMN_TABLE_MISMATCH");
        }

        return column;
    }

    private ColumnTransformationAssignment findOrCreateAssignment(
            TransformationColumn column, ColumnTransformationType type) {

        Optional<ColumnTransformationAssignment> existing = column.getAssignments().stream()
                .filter(a -> a.getTransformationType() == type)
                .findFirst();

        if (existing.isPresent()) {
            return existing.get();
        }

        ColumnTransformationAssignment assignment = new ColumnTransformationAssignment();
        assignment.setTransformationColumn(column);
        assignment.setTransformationType(type);
        return assignment;
    }

    private void checkDuplicateColumnName(String columnName, TransformationTable table, Long excludeColumnId) {
        Set<String> existingColumnNames = getExistingColumnNames(table, excludeColumnId);

        if (existingColumnNames.contains(columnName.toLowerCase())) {
            throw new ValidationException("Column with name '" + columnName + "' already exists in this table",
                    List.of("Duplicate column name: " + columnName));
        }
    }

    private Set<String> getExistingColumnNames(TransformationTable table, Long excludeColumnId) {
        return table.getColumns().stream()
                .filter(column -> !column.getId().equals(excludeColumnId))
                .filter(column -> !isColumnDeleted(column))
                .map(TransformationUtils::getEffectiveColumnName)
                .filter(Objects::nonNull)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }

    private ColumnTransformationAssignment createAddColumnAssignment(TransformationColumn column, ColumnAddRequest request) {
        ColumnTransformationAssignment assignment = new ColumnTransformationAssignment();
        assignment.setTransformationColumn(column);
        assignment.setTransformationType(ColumnTransformationType.ADD_COLUMN);
        assignment.setColumnName(request.getColumnName());
        assignment.setDataType(request.getDataType());
        assignment.setIsNullable(request.getIsNullable());
        assignment.setIsPrimaryKey(request.getIsPrimaryKey());
        assignment.setDefaultValue(request.getDefaultValue());

        return assignment;
    }

    private void updateRelationsForRenamedColumn(Long modelId, String tableName, String oldColumnName, String newColumnName) {
        List<TransformationRelation> relations = relationRepository.findActiveRelationsByColumn(modelId, tableName, oldColumnName);

        for (TransformationRelation relation : relations) {
            boolean updated = false;

            if (relation.getForeignTable().equals(tableName) && relation.getForeignColumn().equals(oldColumnName)) {
                relation.setForeignColumn(newColumnName);
                updated = true;
            }

            if (relation.getPrimaryTable().equals(tableName) && relation.getPrimaryColumn().equals(oldColumnName)) {
                relation.setPrimaryColumn(newColumnName);
                updated = true;
            }

            if (updated) {
                relationRepository.save(relation);
            }
        }
    }

    private TransformationModelDetailsResponse getModelDetailsResponse(Long modelId, Long orgId) {
        entityManager.flush();
        entityManager.clear();
        TransformationModel model = modelRepository.findByIdAndCreatedBy_Organization_Id(modelId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("TransformationModel", modelId));
        return responseMapper.toTransformationModelDetailsResponse(model);
    }

    private void addTypeConversionWarningIfNeeded(TransformationModelDetailsResponse response,
                                                  String sourceType,
                                                  DatabaseTypeEnum sourceDb,
                                                  String targetType,
                                                  DatabaseTypeEnum targetDb,
                                                  String columnName) {

        String warning = typeMappingLoader.getConversionWarning(sourceType, sourceDb, targetType, targetDb);
        if (warning != null) {
            com.database_migrator.domain.execution.dto.ValidationWarning validationWarning =
                    new com.database_migrator.domain.execution.dto.ValidationWarning(
                            "WARNING",
                            warning,
                            columnName
                    );
            if (response.getWarnings() == null) {
                response.setWarnings(new java.util.ArrayList<>());
            }
            response.getWarnings().add(validationWarning);
        }
    }
}
