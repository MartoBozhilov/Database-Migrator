package com.db_migrator.etl_system.mapper;

import com.db_migrator.etl_system.dto.response.*;
import com.db_migrator.etl_system.model.entity.connector.Connector;
import com.db_migrator.etl_system.model.entity.metadata.ColumnMetadata;
import com.db_migrator.etl_system.model.entity.metadata.RelationMetadata;
import com.db_migrator.etl_system.model.entity.metadata.SystemScan;
import com.db_migrator.etl_system.model.entity.metadata.TableMetadata;
import com.db_migrator.etl_system.model.entity.transformation.*;
import com.db_migrator.etl_system.model.entity.user.Organization;
import com.db_migrator.etl_system.model.entity.user.User;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

import static com.db_migrator.etl_system.service.transformation.TransformationUtils.isColumnDeleted;
import static com.db_migrator.etl_system.service.transformation.TransformationUtils.isTableDeleted;

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
        response.setTableCount(tableCount);
        response.setColumnCount(columnCount);

        return response;
    }

    public TransformationModelDetailsResponse toTransformationModelDetailsResponse(TransformationModel model) {
        TransformationModelDetailsResponse response = new TransformationModelDetailsResponse();
        response.setId(model.getId());
        response.setName(model.getName());
        response.setIsConfirmed(model.getIsConfirmed());
        response.setSystemScanId(model.getSystemScan().getId());
        response.setSystemScanName(model.getSystemScan().getName());
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
        response.setTableCount(tableCount);
        response.setColumnCount(columnCount);

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
}
