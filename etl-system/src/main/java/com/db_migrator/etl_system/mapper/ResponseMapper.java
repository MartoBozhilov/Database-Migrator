package com.db_migrator.etl_system.mapper;

import com.db_migrator.etl_system.dto.response.ColumnMetadataResponse;
import com.db_migrator.etl_system.dto.response.ConnectionTestResponse;
import com.db_migrator.etl_system.dto.response.ConnectorResponse;
import com.db_migrator.etl_system.dto.response.OrganizationResponse;
import com.db_migrator.etl_system.dto.response.RelationMetadataResponse;
import com.db_migrator.etl_system.dto.response.SystemScanDetailsResponse;
import com.db_migrator.etl_system.dto.response.SystemScanResponse;
import com.db_migrator.etl_system.dto.response.TableMetadataResponse;
import com.db_migrator.etl_system.dto.response.UserResponse;
import com.db_migrator.etl_system.model.entity.connector.Connector;
import com.db_migrator.etl_system.model.entity.metadata.ColumnMetadata;
import com.db_migrator.etl_system.model.entity.metadata.RelationMetadata;
import com.db_migrator.etl_system.model.entity.metadata.SystemScan;
import com.db_migrator.etl_system.model.entity.metadata.TableMetadata;
import com.db_migrator.etl_system.model.entity.user.Organization;
import com.db_migrator.etl_system.model.entity.user.User;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

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
}
