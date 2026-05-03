package com.db_migrator.etl_system.service.transformation;

import com.db_migrator.etl_system.dto.request.TableTransformationRequest;
import com.db_migrator.etl_system.dto.response.TransformationModelDetailsResponse;
import com.db_migrator.etl_system.mapper.ResponseMapper;
import com.db_migrator.etl_system.model.entity.transformation.ColumnTransformationAssignment;
import com.db_migrator.etl_system.model.entity.transformation.TableTransformationAssignment;
import com.db_migrator.etl_system.model.entity.transformation.TransformationColumn;
import com.db_migrator.etl_system.model.entity.transformation.TransformationModel;
import com.db_migrator.etl_system.model.entity.transformation.TransformationRelation;
import com.db_migrator.etl_system.model.entity.transformation.TransformationTable;
import com.db_migrator.etl_system.model.enums.ColumnTransformationType;
import com.db_migrator.etl_system.model.enums.TableTransformationType;
import com.db_migrator.etl_system.repository.ColumnTransformationAssignmentRepository;
import com.db_migrator.etl_system.repository.TableTransformationAssignmentRepository;
import com.db_migrator.etl_system.repository.TransformationColumnRepository;
import com.db_migrator.etl_system.repository.TransformationModelRepository;
import com.db_migrator.etl_system.repository.TransformationRelationRepository;
import com.db_migrator.etl_system.repository.TransformationTableRepository;
import com.db_migrator.etl_system.security.SecurityUtils;
import com.db_migrator.etl_system.service.transformation.validation.SqlIdentifierValidator;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TransformationTableService {

    private final TransformationModelRepository modelRepository;
    private final TransformationTableRepository tableRepository;
    private final TransformationColumnRepository columnRepository;
    private final TableTransformationAssignmentRepository assignmentRepository;
    private final ColumnTransformationAssignmentRepository columnAssignmentRepository;
    private final TransformationRelationRepository relationRepository;
    private final SqlIdentifierValidator sqlIdentifierValidator;
    private final SecurityUtils securityUtils;
    private final ResponseMapper responseMapper;
    private final EntityManager entityManager;

    @Transactional
    public TransformationModelDetailsResponse renameTable(Long modelId, Long tableId, String newName) {
        Long orgId = securityUtils.getCurrentOrganizationId();

        TransformationModel model = modelRepository.findByIdAndCreatedBy_Organization_Id(modelId, orgId)
                .orElseThrow(() -> new RuntimeException("Transformation model not found"));

        TransformationTable table = getTableAndValidateOwnership(tableId, modelId);

        validateTableName(newName, model);

        // Check for duplicate table name (excluding current table)
        checkDuplicateTableName(newName, model, tableId);

        // Get current table name before renaming
        String oldTableName = getEffectiveTableName(table);

        // Find or create RENAME_TABLE assignment
        TableTransformationAssignment assignment = findOrCreateAssignment(table, TableTransformationType.RENAME_TABLE);
        assignment.setNewName(newName);
        assignmentRepository.save(assignment);

        // Update all relations that reference this table
        if (oldTableName != null) {
            updateRelationsForRenamedTable(model.getId(), oldTableName, newName);
        }

        return getModelDetailsResponse(modelId, orgId);
    }

    @Transactional
    public TransformationModelDetailsResponse addTable(Long modelId, TableTransformationRequest request) {
        Long orgId = securityUtils.getCurrentOrganizationId();

        TransformationModel model = modelRepository.findByIdAndCreatedBy_Organization_Id(modelId, orgId)
                .orElseThrow(() -> new RuntimeException("Transformation model not found"));

        validateAddTableRequest(request, model);

        // Check for duplicate table name
        checkDuplicateTableName(request.getTableName(), model, null);

        // Create new table with ADD_TABLE assignment
        TransformationTable table = createNewTable(model, request);

        // Create ID column for the new table
        createIdColumn(table, request.getIdColumnName(), request.getIdColumnDataType());

        return getModelDetailsResponse(modelId, orgId);
    }

    @Transactional
    public TransformationModelDetailsResponse deleteTable(Long modelId, Long tableId) {
        Long orgId = securityUtils.getCurrentOrganizationId();

        TransformationModel model = modelRepository.findByIdAndCreatedBy_Organization_Id(modelId, orgId)
                .orElseThrow(() -> new RuntimeException("Transformation model not found"));

        TransformationTable table = getTableAndValidateOwnership(tableId, modelId);

        // Get actual table name (check for RENAME_TABLE assignment first)
        String tableName = getEffectiveTableName(table);

        // Check if table has active relationships
        checkTableRelationships(model.getId(), tableName);

        // Find or create DELETE_TABLE assignment
        TableTransformationAssignment assignment = findOrCreateAssignment(table, TableTransformationType.DELETE_TABLE);
        assignmentRepository.save(assignment);

        return getModelDetailsResponse(modelId, orgId);
    }

    private void validateTableName(String tableName, TransformationModel model) {
        if (tableName == null || tableName.trim().isEmpty()) {
            throw new RuntimeException("Table name is required");
        }
        sqlIdentifierValidator.validateIdentifier(tableName, model.getTargetConnector().getDatabaseType());
    }

    private void validateAddTableRequest(TableTransformationRequest request, TransformationModel model) {
        if (request.getTableName() == null || request.getTableName().trim().isEmpty()) {
            throw new RuntimeException("Table name is required");
        }
        if (request.getIdColumnName() == null || request.getIdColumnName().trim().isEmpty()) {
            throw new RuntimeException("ID column name is required");
        }
        if (request.getIdColumnDataType() == null || request.getIdColumnDataType().trim().isEmpty()) {
            throw new RuntimeException("ID column data type is required");
        }
        sqlIdentifierValidator.validateIdentifier(request.getTableName(), model.getTargetConnector().getDatabaseType());
        sqlIdentifierValidator.validateIdentifier(request.getIdColumnName(), model.getTargetConnector().getDatabaseType());
    }

    private TransformationTable createNewTable(TransformationModel model, TableTransformationRequest request) {
        // Create new TransformationTable (sourceTableMetadata = null for user-created tables)
        TransformationTable table = new TransformationTable();
        table.setTransformationModel(model);
        table.setSourceTableMetadata(null);
        table = tableRepository.save(table);

        // Create ADD_TABLE assignment
        TableTransformationAssignment assignment = new TableTransformationAssignment();
        assignment.setTransformationTable(table);
        assignment.setTransformationType(TableTransformationType.ADD_TABLE);
        assignment.setTableName(request.getTableName());
        assignment.setIdGenerationStrategy(request.getIdGenerationStrategy());
        assignmentRepository.save(assignment);

        return table;
    }

    private void createIdColumn(TransformationTable table, String idColumnName, String idColumnDataType) {
        TransformationColumn idColumn = new TransformationColumn();
        idColumn.setTransformationTable(table);
        idColumn.setSourceColumnMetadata(null);
        idColumn = columnRepository.save(idColumn);

        ColumnTransformationAssignment columnAssignment = new ColumnTransformationAssignment();
        columnAssignment.setTransformationColumn(idColumn);
        columnAssignment.setTransformationType(ColumnTransformationType.ADD_COLUMN);
        columnAssignment.setColumnName(idColumnName);
        columnAssignment.setDataType(idColumnDataType);
        columnAssignment.setIsNullable(false);
        columnAssignment.setIsPrimaryKey(true);
        columnAssignment.setDefaultValue(null);

        columnAssignmentRepository.save(columnAssignment);
    }

    private TransformationTable getTableAndValidateOwnership(Long tableId, Long modelId) {
        TransformationTable table = tableRepository.findById(tableId)
                .orElseThrow(() -> new RuntimeException("Transformation table not found"));

        if (!table.getTransformationModel().getId().equals(modelId)) {
            throw new RuntimeException("Table does not belong to this transformation model");
        }

        return table;
    }

    private TableTransformationAssignment findOrCreateAssignment(
            TransformationTable table, TableTransformationType type) {

        Optional<TableTransformationAssignment> existing = table.getAssignments().stream()
                .filter(a -> a.getTransformationType() == type)
                .findFirst();

        if (existing.isPresent()) {
            return existing.get();
        }

        TableTransformationAssignment assignment = new TableTransformationAssignment();
        assignment.setTransformationTable(table);
        assignment.setTransformationType(type);
        return assignment;
    }

    private String getEffectiveTableName(TransformationTable table) {
        // Check if table has RENAME_TABLE assignment
        Optional<TableTransformationAssignment> renameAssignment = table.getAssignments().stream()
                .filter(a -> a.getTransformationType() == TableTransformationType.RENAME_TABLE)
                .findFirst();

        if (renameAssignment.isPresent() && renameAssignment.get().getNewName() != null) {
            return renameAssignment.get().getNewName();
        }

        // Check if table was added by user (ADD_TABLE)
        Optional<TableTransformationAssignment> addAssignment = table.getAssignments().stream()
                .filter(a -> a.getTransformationType() == TableTransformationType.ADD_TABLE)
                .findFirst();

        if (addAssignment.isPresent() && addAssignment.get().getTableName() != null) {
            return addAssignment.get().getTableName();
        }

        // Return original name from metadata
        return table.getSourceTableMetadata() != null
                ? table.getSourceTableMetadata().getTableName()
                : null;
    }

    private void checkTableRelationships(Long modelId, String tableName) {
        if (tableName == null) {
            return;
        }

        // Find all active relations involving this table
        List<TransformationRelation> relations = relationRepository.findActiveRelationsByTable(modelId, tableName);

        if (!relations.isEmpty()) {
            // Separate relations by type (foreign key side vs primary key side)
            List<TransformationRelation> foreignKeyRelations = relations.stream()
                    .filter(r -> r.getForeignTable().equals(tableName))
                    .toList();

            List<TransformationRelation> primaryKeyRelations = relations.stream()
                    .filter(r -> r.getPrimaryTable().equals(tableName))
                    .toList();

            // Build detailed error message
            StringBuilder errorMessage = new StringBuilder();
            errorMessage.append("Cannot delete table '").append(tableName)
                    .append("' because it has ").append(relations.size())
                    .append(" active foreign key relationship(s). ");
            errorMessage.append("Please delete the following relationship(s) first:\n\n");

            // Show foreign key relationships (this table references other tables)
            if (!foreignKeyRelations.isEmpty()) {
                errorMessage.append("Foreign Key Relationships (this table references others):\n");
                for (TransformationRelation relation : foreignKeyRelations) {
                    errorMessage.append("  - Column '")
                            .append(relation.getForeignColumn())
                            .append("' references ")
                            .append(relation.getPrimaryTable())
                            .append(".")
                            .append(relation.getPrimaryColumn())
                            .append("\n");
                }
                errorMessage.append("\n");
            }

            // Show primary key relationships (other tables reference this table)
            if (!primaryKeyRelations.isEmpty()) {
                errorMessage.append("Primary Key Relationships (other tables reference this table):\n");
                for (TransformationRelation relation : primaryKeyRelations) {
                    errorMessage.append("  - Column '")
                            .append(relation.getPrimaryColumn())
                            .append("' is referenced by ")
                            .append(relation.getForeignTable())
                            .append(".")
                            .append(relation.getForeignColumn())
                            .append("\n");
                }
            }

            throw new RuntimeException(errorMessage.toString());
        }
    }

    private void updateRelationsForRenamedTable(Long modelId, String oldTableName, String newTableName) {
        // Find all relations involving the old table name
        List<TransformationRelation> relations = relationRepository.findActiveRelationsByTable(modelId, oldTableName);

        for (TransformationRelation relation : relations) {
            // Update foreign table name if it matches
            if (relation.getForeignTable().equals(oldTableName)) {
                relation.setForeignTable(newTableName);
            }

            // Update primary table name if it matches
            if (relation.getPrimaryTable().equals(oldTableName)) {
                relation.setPrimaryTable(newTableName);
            }

            relationRepository.save(relation);
        }
    }

    private void checkDuplicateTableName(String tableName, TransformationModel model, Long excludeTableId) {
        Set<String> existingTableNames = new HashSet<>();

        for (TransformationTable table : model.getTransformationTables()) {
            // Skip the table being renamed
            if (table.getId().equals(excludeTableId)) {
                continue;
            }

            // Skip deleted tables
            boolean isDeleted = table.getAssignments().stream()
                    .anyMatch(a -> a.getTransformationType() == TableTransformationType.DELETE_TABLE);
            if (isDeleted) {
                continue;
            }

            String effectiveName = getEffectiveTableName(table);
            if (effectiveName != null) {
                existingTableNames.add(effectiveName.toLowerCase());
            }
        }

        if (existingTableNames.contains(tableName.toLowerCase())) {
            throw new RuntimeException("Table with name '" + tableName + "' already exists in this transformation model");
        }
    }

    private TransformationModelDetailsResponse getModelDetailsResponse(Long modelId, Long orgId) {
        entityManager.flush(); // Ensure all changes are persisted
        entityManager.clear(); // Clear persistence context to force reload
        TransformationModel model = modelRepository.findByIdAndCreatedBy_Organization_Id(modelId, orgId)
                .orElseThrow(() -> new RuntimeException("Transformation model not found"));
        return responseMapper.toTransformationModelDetailsResponse(model);
    }
}
