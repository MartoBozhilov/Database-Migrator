package com.database_migrator.domain.common.mapper;

import com.database_migrator.domain.auth.dto.OrganizationResponse;
import com.database_migrator.domain.auth.dto.UserResponse;
import com.database_migrator.domain.common.util.TransformationUtils;
import com.database_migrator.domain.connector.dto.ConnectionTestResponse;
import com.database_migrator.domain.connector.dto.ConnectorResponse;
import com.database_migrator.domain.execution.dto.CycleDetailsResponse;
import com.database_migrator.domain.execution.dto.CycleResponse;
import com.database_migrator.domain.execution.dto.TaskResponse;
import com.database_migrator.domain.execution.model.Cycle;
import com.database_migrator.domain.execution.model.Task;
import com.database_migrator.domain.execution.model.TaskStatusEnum;
import com.database_migrator.domain.scan.dto.ColumnMetadataResponse;
import com.database_migrator.domain.scan.dto.RelationMetadataResponse;
import com.database_migrator.domain.scan.dto.SystemScanDetailsResponse;
import com.database_migrator.domain.scan.dto.SystemScanResponse;
import com.database_migrator.domain.scan.dto.TableMetadataResponse;
import com.database_migrator.domain.transformation.dto.ColumnTransformationAssignmentResponse;
import com.database_migrator.domain.transformation.dto.TableTransformationAssignmentResponse;
import com.database_migrator.domain.transformation.dto.TransformationColumnResponse;
import com.database_migrator.domain.transformation.dto.TransformationModelDetailsResponse;
import com.database_migrator.domain.transformation.dto.TransformationModelResponse;
import com.database_migrator.domain.transformation.dto.TransformationRelationResponse;
import com.database_migrator.domain.transformation.dto.TransformationTableResponse;
import com.database_migrator.domain.transformation.model.ColumnTransformationAssignment;
import com.database_migrator.domain.transformation.model.ColumnTransformationType;
import com.database_migrator.domain.transformation.model.TableTransformationAssignment;
import com.database_migrator.domain.transformation.model.TransformationColumn;
import com.database_migrator.domain.transformation.model.TransformationModel;
import com.database_migrator.domain.transformation.model.TransformationRelation;
import com.database_migrator.domain.transformation.model.TransformationTable;
import com.database_migrator.domain.connector.model.Connector;
import com.database_migrator.domain.scan.model.ColumnMetadata;
import com.database_migrator.domain.scan.model.RelationMetadata;
import com.database_migrator.domain.scan.model.SystemScan;
import com.database_migrator.domain.scan.model.TableMetadata;
import com.database_migrator.domain.auth.model.Organization;
import com.database_migrator.domain.auth.model.User;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

import static com.database_migrator.domain.common.util.TransformationUtils.isColumnDeleted;
import static com.database_migrator.domain.common.util.TransformationUtils.isTableDeleted;

@Component
public class ResponseMapper {

    public OrganizationResponse toOrganizationResponse(Organization organization) {
        return OrganizationResponse.builder()
                .id(organization.getId())
                .name(organization.getName())
                .companyName(organization.getCompanyName())
                .location(organization.getLocation())
                .createdAt(organization.getCreatedAt().toString())
                .build();
    }

    public UserResponse toUserResponse(User user) {
        List<String> roles = user.getRoles().stream()
                .map(role -> role.getRole().name())
                .collect(Collectors.toList());

        OrganizationResponse organizationResponse = toOrganizationResponse(user.getOrganization());

        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .organization(organizationResponse)
                .roles(roles)
                .createdAt(user.getCreatedAt().toString())
                .build();
    }

    public ConnectorResponse toConnectorResponse(Connector connector) {
        ConnectorResponse response = new ConnectorResponse();
        response.setId(connector.getId());
        response.setName(connector.getName());
        response.setDatabaseType(connector.getDatabaseType());
        response.setConnectorType(connector.getConnectorType());
        response.setHost(connector.getHost());
        response.setPort(connector.getPort());
        response.setDatabaseName(connector.getDatabaseName());
        response.setUsername(connector.getUsername());
        response.setCreatedById(connector.getCreatedBy().getId());
        response.setCreatedByName(connector.getCreatedBy().getUsername());
        response.setCreatedAt(connector.getCreatedAt());
        response.setUpdatedAt(connector.getUpdatedAt());
        return response;
    }

    public SystemScanResponse toSystemScanResponse(SystemScan scan) {
        SystemScanResponse response = new SystemScanResponse();
        response.setId(scan.getId());
        response.setName(scan.getName());
        response.setSourceConnectorId(scan.getSourceConnector().getId());
        response.setSourceConnectorName(scan.getSourceConnector().getName());
        response.setStatus(scan.getStatus());
        response.setCreatedById(scan.getCreatedBy().getId());
        response.setCreatedByName(scan.getCreatedBy().getUsername());
        response.setCreatedAt(scan.getCreatedAt());
        response.setStartedAt(scan.getStartedAt());
        response.setCompletedAt(scan.getCompletedAt());
        response.setErrorMessage(scan.getErrorMessage());
        response.setTableCount(scan.getTableMetadataList() != null ? scan.getTableMetadataList().size() : 0);
        response.setRelationCount(scan.getRelationMetadataList() != null ? scan.getRelationMetadataList().size() : 0);
        return response;
    }

    public SystemScanDetailsResponse toSystemScanDetailsResponse(SystemScan scan) {
        SystemScanDetailsResponse response = new SystemScanDetailsResponse();
        response.setId(scan.getId());
        response.setName(scan.getName());
        response.setSourceConnectorId(scan.getSourceConnector().getId());
        response.setSourceConnectorName(scan.getSourceConnector().getName());
        response.setStatus(scan.getStatus());
        response.setCreatedById(scan.getCreatedBy().getId());
        response.setCreatedByName(scan.getCreatedBy().getUsername());
        response.setCreatedAt(scan.getCreatedAt());
        response.setStartedAt(scan.getStartedAt());
        response.setCompletedAt(scan.getCompletedAt());
        response.setErrorMessage(scan.getErrorMessage());
        response.setTableCount(scan.getTableMetadataList() != null ? scan.getTableMetadataList().size() : 0);
        response.setRelationCount(scan.getRelationMetadataList() != null ? scan.getRelationMetadataList().size() : 0);

        List<TableMetadataResponse> tables = scan.getTableMetadataList().stream()
                .map(this::toTableMetadataResponse)
                .collect(Collectors.toList());
        response.setTables(tables);

        List<RelationMetadataResponse> relations = scan.getRelationMetadataList().stream()
                .map(this::toRelationMetadataResponse)
                .collect(Collectors.toList());
        response.setRelations(relations);

        return response;
    }

    public TableMetadataResponse toTableMetadataResponse(TableMetadata table) {
        TableMetadataResponse response = new TableMetadataResponse();
        response.setId(table.getId());
        response.setTableName(table.getTableName());

        List<ColumnMetadataResponse> columns = table.getColumnMetadataList().stream()
                .map(this::toColumnMetadataResponse)
                .collect(Collectors.toList());
        response.setColumns(columns);

        return response;
    }

    public ColumnMetadataResponse toColumnMetadataResponse(ColumnMetadata column) {
        ColumnMetadataResponse response = new ColumnMetadataResponse();
        response.setId(column.getId());
        response.setColumnName(column.getColumnName());
        response.setDataType(column.getDataType());
        response.setIsNullable(column.getIsNullable());
        response.setLength(column.getLength());
        response.setIsPrimaryKey(column.getIsPrimaryKey());
        response.setIsAutoIncrement(column.getIsAutoIncrement());
        return response;
    }

    public RelationMetadataResponse toRelationMetadataResponse(RelationMetadata relation) {
        RelationMetadataResponse response = new RelationMetadataResponse();
        response.setId(relation.getId());
        response.setForeignTable(relation.getForeignTable());
        response.setForeignColumn(relation.getForeignColumn());
        response.setPrimaryTable(relation.getPrimaryTable());
        response.setPrimaryColumn(relation.getPrimaryColumn());
        return response;
    }

    public ConnectionTestResponse toConnectionTestResponse(boolean success, String message, String databaseVersion) {
        ConnectionTestResponse response = new ConnectionTestResponse();
        response.setSuccess(success);
        response.setMessage(message);
        response.setDatabaseVersion(databaseVersion);
        return response;
    }

    public TransformationModelResponse toTransformationModelResponse(TransformationModel model) {
        TransformationModelResponse response = new TransformationModelResponse();
        response.setId(model.getId());
        response.setName(model.getName());
        response.setIsConfirmed(model.getIsConfirmed());
        response.setSystemScanId(model.getSystemScan().getId());
        response.setSystemScanName(model.getSystemScan().getName());
        response.setSourceConnectorId(model.getSystemScan().getSourceConnector().getId());
        response.setSourceConnectorName(model.getSystemScan().getSourceConnector().getName());
        response.setSourceDatabaseType(model.getSystemScan().getSourceConnector().getDatabaseType());
        response.setTargetConnectorId(model.getTargetConnector().getId());
        response.setTargetConnectorName(model.getTargetConnector().getName());
        response.setTargetDatabaseType(model.getTargetConnector().getDatabaseType());
        response.setCreatedById(model.getCreatedBy().getId());
        response.setCreatedByName(model.getCreatedBy().getUsername());
        response.setCreatedAt(model.getCreatedAt());

        int tableCount = model.getTransformationTables() != null ? model.getTransformationTables().size() : 0;
        int columnCount = 0;
        if (model.getTransformationTables() != null) {
            for (TransformationTable table : model.getTransformationTables()) {
                if (table.getColumns() != null) {
                    columnCount += table.getColumns().size();
                }
            }
        }
        int relationCount = model.getTransformationRelations() != null ? model.getTransformationRelations().size() : 0;
        response.setTableCount(tableCount);
        response.setColumnCount(columnCount);
        response.setRelationCount(relationCount);

        return response;
    }

    public TransformationModelDetailsResponse toTransformationModelDetailsResponse(TransformationModel model) {
        TransformationModelDetailsResponse response = new TransformationModelDetailsResponse();
        response.setId(model.getId());
        response.setName(model.getName());
        response.setIsConfirmed(model.getIsConfirmed());
        response.setSystemScanId(model.getSystemScan().getId());
        response.setSystemScanName(model.getSystemScan().getName());
        response.setSourceConnectorId(model.getSystemScan().getSourceConnector().getId());
        response.setSourceConnectorName(model.getSystemScan().getSourceConnector().getName());
        response.setSourceDatabaseType(model.getSystemScan().getSourceConnector().getDatabaseType());
        response.setTargetConnectorId(model.getTargetConnector().getId());
        response.setTargetConnectorName(model.getTargetConnector().getName());
        response.setTargetDatabaseType(model.getTargetConnector().getDatabaseType());
        response.setCreatedById(model.getCreatedBy().getId());
        response.setCreatedByName(model.getCreatedBy().getUsername());
        response.setCreatedAt(model.getCreatedAt());

        // Filter out soft-deleted tables
        List<TransformationTable> activeTables = model.getTransformationTables() != null ?
                model.getTransformationTables().stream()
                        .filter(table -> !isTableDeleted(table))
                        .toList() : List.of();

        int tableCount = activeTables.size();
        int columnCount = 0;
        for (TransformationTable table : activeTables) {
            if (table.getColumns() != null) {
                // Only count non-deleted columns
                columnCount += (int) table.getColumns().stream()
                        .filter(column -> !isColumnDeleted(column))
                        .count();
            }
        }
        int relationCount = model.getTransformationRelations() != null ?
                (int) model.getTransformationRelations().stream()
                        .filter(relation -> !relation.getIsDeleted())
                        .count() : 0;
        response.setTableCount(tableCount);
        response.setColumnCount(columnCount);
        response.setRelationCount(relationCount);

        List<TransformationTableResponse> tables = activeTables.stream()
                .map(this::toTransformationTableResponse)
                .collect(Collectors.toList());
        response.setTables(tables);

        // Filter out soft-deleted relations
        List<TransformationRelationResponse> relations = model.getTransformationRelations() != null ?
                model.getTransformationRelations().stream()
                        .filter(relation -> !relation.getIsDeleted())
                        .map(this::toTransformationRelationResponse)
                        .collect(Collectors.toList()) : List.of();
        response.setRelations(relations);

        response.setWarnings(List.of());

        return response;
    }

    public TransformationTableResponse toTransformationTableResponse(TransformationTable table) {
        TransformationTableResponse response = new TransformationTableResponse();
        response.setId(table.getId());
        response.setSourceTableName(table.getSourceTableMetadata() != null ?
                table.getSourceTableMetadata().getTableName() : null);
        response.setSourceTableMetadataId(table.getSourceTableMetadata() != null ?
                table.getSourceTableMetadata().getId() : null);

        List<TableTransformationAssignmentResponse> tableTransformations = table.getAssignments() != null ?
                table.getAssignments().stream()
                        .map(this::toTableTransformationAssignmentResponse)
                        .collect(Collectors.toList()) : List.of();
        response.setTableTransformations(tableTransformations);

        // Filter out soft-deleted columns
        List<TransformationColumnResponse> columns = table.getColumns() != null ?
                table.getColumns().stream()
                        .filter(column -> !isColumnDeleted(column))
                        .map(this::toTransformationColumnResponse)
                        .collect(Collectors.toList()) : List.of();
        response.setColumns(columns);

        return response;
    }

    public TransformationColumnResponse toTransformationColumnResponse(TransformationColumn column) {
        TransformationColumnResponse response = new TransformationColumnResponse();
        response.setId(column.getId());
        response.setSourceColumnName(column.getSourceColumnMetadata() != null ?
                column.getSourceColumnMetadata().getColumnName() : null);
        response.setSourceDataType(column.getSourceColumnMetadata() != null ?
                column.getSourceColumnMetadata().getDataType() : null);
        response.setResolvedTargetType(column.getResolvedTargetType());
        response.setSourceColumnMetadataId(column.getSourceColumnMetadata() != null ?
                column.getSourceColumnMetadata().getId() : null);

        // Set PK flag - check ADD_COLUMN assignment first (for user-added columns), then metadata
        boolean isPrimaryKey = false;
        if (column.getAssignments() != null) {
            for (ColumnTransformationAssignment assignment : column.getAssignments()) {
                if (assignment.getTransformationType() == ColumnTransformationType.ADD_COLUMN) {
                    isPrimaryKey = Boolean.TRUE.equals(assignment.getIsPrimaryKey());
                    break;
                }
            }
        }
        // Fallback to source metadata for scanned columns
        if (!isPrimaryKey && column.getSourceColumnMetadata() != null) {
            isPrimaryKey = Boolean.TRUE.equals(column.getSourceColumnMetadata().getIsPrimaryKey());
        }
        response.setIsPrimaryKey(isPrimaryKey);

        // Set FK flag by checking if column is used in any relation
        boolean isForeignKey = false;
        if (column.getTransformationTable() != null &&
            column.getTransformationTable().getTransformationModel() != null &&
            column.getTransformationTable().getTransformationModel().getTransformationRelations() != null) {

            String effectiveTableName = TransformationUtils.getEffectiveTableName(column.getTransformationTable());
            String effectiveColumnName = TransformationUtils.getEffectiveColumnName(column);

            if (effectiveTableName != null && effectiveColumnName != null) {
                isForeignKey = column.getTransformationTable().getTransformationModel()
                        .getTransformationRelations().stream()
                        .anyMatch(rel -> !rel.getIsDeleted() &&
                                effectiveTableName.equals(rel.getForeignTable()) &&
                                effectiveColumnName.equals(rel.getForeignColumn()));
            }
        }
        response.setIsForeignKey(isForeignKey);

        List<ColumnTransformationAssignmentResponse> columnTransformations = column.getAssignments() != null ?
                column.getAssignments().stream()
                        .map(this::toColumnTransformationAssignmentResponse)
                        .collect(Collectors.toList()) : List.of();
        response.setColumnTransformations(columnTransformations);

        return response;
    }

    public TableTransformationAssignmentResponse toTableTransformationAssignmentResponse(TableTransformationAssignment trans) {
        TableTransformationAssignmentResponse response = new TableTransformationAssignmentResponse();
        response.setId(trans.getId());
        response.setTransformationType(trans.getTransformationType());

        // RENAME_TABLE
        response.setNewName(trans.getNewName());

        // ADD_TABLE
        response.setTableName(trans.getTableName());
        response.setIdGenerationStrategy(trans.getIdGenerationStrategy());

        return response;
    }

    public ColumnTransformationAssignmentResponse toColumnTransformationAssignmentResponse(ColumnTransformationAssignment trans) {
        ColumnTransformationAssignmentResponse response = new ColumnTransformationAssignmentResponse();
        response.setId(trans.getId());
        response.setTransformationType(trans.getTransformationType());

        // RENAME_COLUMN
        response.setNewName(trans.getNewName());

        // ADD_COLUMN
        response.setColumnName(trans.getColumnName());
        response.setDataType(trans.getDataType());
        response.setIsNullable(trans.getIsNullable());
        response.setIsPrimaryKey(trans.getIsPrimaryKey());
        response.setDefaultValue(trans.getDefaultValue());

        // CHANGE_TYPE
        response.setTargetDataType(trans.getTargetDataType());

        return response;
    }

    public TransformationRelationResponse toTransformationRelationResponse(TransformationRelation relation) {
        TransformationRelationResponse response = new TransformationRelationResponse();
        response.setId(relation.getId());
        response.setSourceRelationMetadataId(relation.getSourceRelationMetadata() != null ?
            relation.getSourceRelationMetadata().getId() : null);
        response.setIsDeleted(relation.getIsDeleted());
        response.setForeignTable(relation.getForeignTable());
        response.setForeignColumn(relation.getForeignColumn());
        response.setPrimaryTable(relation.getPrimaryTable());
        response.setPrimaryColumn(relation.getPrimaryColumn());
        return response;
    }

    // ===== Cycle Mapping Methods =====

    public CycleResponse toCycleResponse(Cycle cycle) {
        CycleResponse response = new CycleResponse();
        response.setId(cycle.getId());
        response.setName(cycle.getName());
        response.setTransformationModelId(cycle.getTransformationModel().getId());
        response.setTransformationModelName(cycle.getTransformationModel().getName());

        response.setTargetConnectorId(cycle.getTargetConnector().getId());
        response.setTargetConnectorName(cycle.getTargetConnector().getName());

        response.setStatus(cycle.getStatus());
        response.setCreatedAt(cycle.getCreatedAt());
        response.setStartedAt(cycle.getStartedAt());
        response.setCompletedAt(cycle.getCompletedAt());
        response.setErrorMessage(cycle.getErrorMessage());
        response.setCreatedById(cycle.getCreatedBy().getId());
        response.setCreatedByName(cycle.getCreatedBy().getUsername());

        // Count tasks by status
        if (cycle.getTasks() != null) {
            response.setTotalTasks(cycle.getTasks().size());
            response.setTaskCount(cycle.getTasks().size()); // Alias for UI
            response.setCompletedTasks((int) cycle.getTasks().stream()
                .filter(t -> t.getStatus() == TaskStatusEnum.COMPLETED)
                .count());
            response.setFailedTasks((int) cycle.getTasks().stream()
                .filter(t -> t.getStatus() == TaskStatusEnum.FAILED)
                .count());
        }

        return response;
    }

    public CycleDetailsResponse toCycleDetailsResponse(Cycle cycle) {
        CycleDetailsResponse response = new CycleDetailsResponse();
        response.setId(cycle.getId());
        response.setName(cycle.getName());
        response.setTransformationModelId(cycle.getTransformationModel().getId());
        response.setTransformationModelName(cycle.getTransformationModel().getName());
        response.setStatus(cycle.getStatus());
        response.setCreatedAt(cycle.getCreatedAt());
        response.setStartedAt(cycle.getStartedAt());
        response.setCompletedAt(cycle.getCompletedAt());
        response.setErrorMessage(cycle.getErrorMessage());
        response.setCreatedById(cycle.getCreatedBy().getId());
        response.setCreatedByName(cycle.getCreatedBy().getUsername());

        // Count tasks by status
        if (cycle.getTasks() != null) {
            response.setTotalTasks(cycle.getTasks().size());
            response.setCompletedTasks((int) cycle.getTasks().stream()
                .filter(t -> t.getStatus() == TaskStatusEnum.COMPLETED)
                .count());
            response.setFailedTasks((int) cycle.getTasks().stream()
                .filter(t -> t.getStatus() == TaskStatusEnum.FAILED)
                .count());

            // Map tasks
            response.setTasks(cycle.getTasks().stream()
                .map(this::toTaskResponse)
                .collect(Collectors.toList()));
        }

        return response;
    }

    public TaskResponse toTaskResponse(Task task) {
        TaskResponse response = new TaskResponse();
        response.setId(task.getId());

        // Derive table name from TransformationTable
        String tableName = TransformationUtils
            .getEffectiveTableName(task.getTransformationTable());
        response.setTableName(tableName);

        response.setStatus(task.getStatus());
        response.setStartedAt(task.getStartedAt());
        response.setCompletedAt(task.getCompletedAt());
        response.setErrorMessage(task.getErrorMessage());
        response.setRowsProcessed(task.getRowsProcessed());

        // Map dependencies (table names)
        if (task.getDependencies() != null) {
            response.setDependsOn(task.getDependencies().stream()
                .map(depTask -> TransformationUtils
                    .getEffectiveTableName(depTask.getTransformationTable()))
                .collect(Collectors.toList()));
        }

        return response;
    }
}

