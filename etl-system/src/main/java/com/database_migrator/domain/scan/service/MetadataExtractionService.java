package com.database_migrator.domain.scan.service;

import com.database_migrator.config.database.MetadataQueryConfig;
import com.database_migrator.domain.connector.model.Connector;
import com.database_migrator.domain.scan.model.ColumnMetadata;
import com.database_migrator.domain.scan.model.RelationMetadata;
import com.database_migrator.domain.scan.model.SystemScan;
import com.database_migrator.domain.scan.model.TableMetadata;
import com.database_migrator.domain.scan.model.ScanStatusEnum;
import com.database_migrator.domain.scan.repository.ColumnMetadataRepository;
import com.database_migrator.domain.scan.repository.RelationMetadataRepository;
import com.database_migrator.domain.scan.repository.SystemScanRepository;
import com.database_migrator.domain.scan.repository.TableMetadataRepository;
import com.database_migrator.config.database.MetadataQueryLoader;
import com.database_migrator.domain.common.util.DatabaseConnectionManager;
import com.database_migrator.domain.common.exception.ResourceNotFoundException;
import com.database_migrator.domain.common.exception.ExecutionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MetadataExtractionService {

    private final SystemScanRepository scanRepository;
    private final TableMetadataRepository tableRepository;
    private final ColumnMetadataRepository columnRepository;
    private final RelationMetadataRepository relationRepository;
    private final MetadataQueryLoader queryLoader;
    private final DatabaseConnectionManager connectionManager;

    @Async("metadataExtractionTaskExecutor")
    @Transactional
    public void extractMetadataAsync(Long scanId) {
        log.info("Starting metadata extraction for scan ID: {}", scanId);

        try {
            SystemScan scan = scanRepository.findById(scanId)
                    .orElseThrow(() -> new ResourceNotFoundException("SystemScan", scanId));

            Connector connector = scan.getSourceConnector();
            String schemaOrDatabase = getSchemaOrDatabase(connector);

            updateScanStatus(scanId, ScanStatusEnum.RUNNING, new Date(), null, null);

            Connection connection = connectionManager.createConnection(connector);

            try {
                MetadataQueryConfig queryConfig = queryLoader.getQueryConfig(connector.getDatabaseType());

                Map<String, TableMetadata> tablesMap = extractTablesAndColumns(
                        connection, scan, queryConfig, schemaOrDatabase);
                log.info("Extracted {} tables with columns for scan ID: {}", tablesMap.size(), scanId);

                List<RelationMetadata> relations = extractRelations(
                        connection, scan, queryConfig, schemaOrDatabase);
                relationRepository.saveAll(relations);
                log.info("Extracted {} relations for scan ID: {}", relations.size(), scanId);

                updateScanStatus(scanId, ScanStatusEnum.COMPLETED, null, new Date(), null);
                log.info("Metadata extraction completed successfully for scan ID: {}", scanId);

            } catch (SQLException e) {
                log.error("SQL error during metadata extraction for scan ID: {}", scanId, e);
                updateScanStatus(scanId, ScanStatusEnum.FAILED, null, new Date(), e.getMessage());
                throw new ExecutionException("Metadata extraction failed", e);
            } finally {
                connectionManager.closeQuietly(connection);
            }

        } catch (Exception e) {
            log.error("Unexpected error during metadata extraction for scan ID: {}", scanId, e);
            updateScanStatus(scanId, ScanStatusEnum.FAILED, null, new Date(), e.getMessage());
        }
    }

    public Map<String, String> testConnection(Connector connector) {
        Map<String, String> result = new HashMap<>();
        try {
            Connection connection = connectionManager.createConnection(connector);

            try {
                java.sql.DatabaseMetaData metaData = connection.getMetaData();
                String databaseVersion = metaData.getDatabaseProductName() + " " + metaData.getDatabaseProductVersion();

                result.put("success", "true");
                result.put("message", "Connection successful");
                result.put("databaseVersion", databaseVersion);

            } finally {
                connectionManager.closeQuietly(connection);
            }

        } catch (Exception e) {
            result.put("success", "false");
            result.put("message", "Connection failed: " + e.getMessage());
        }
        return result;
    }

    @Transactional
    private void updateScanStatus(Long scanId, ScanStatusEnum status, Date startedAt, Date completedAt,
                                 String errorMessage) {
        try {
            if (status == ScanStatusEnum.RUNNING && startedAt != null) {
                scanRepository.updateScanToRunning(scanId, status.name(), startedAt);
                log.info("Updated scan {} to RUNNING status", scanId);
            } else if (status == ScanStatusEnum.COMPLETED && completedAt != null) {
                scanRepository.updateScanToCompleted(scanId, status.name(), completedAt);
                log.info("Updated scan {} to COMPLETED status", scanId);
            } else if (status == ScanStatusEnum.FAILED && completedAt != null) {
                scanRepository.updateScanToFailed(scanId, status.name(), completedAt, errorMessage);
                log.error("Updated scan {} to FAILED status: {}", scanId, errorMessage);
            }
        } catch (Exception e) {
            log.error("Failed to update scan status for scan {}", scanId, e);
        }
    }

    private String getSchemaOrDatabase(Connector connector) {
        return switch (connector.getDatabaseType()) {
            case MYSQL -> connector.getDatabaseName();
            case POSTGRESQL -> "public";
            case MSSQL -> "dbo";
        };
    }

    private Map<String, TableMetadata> extractTablesAndColumns(
            Connection connection, SystemScan scan, MetadataQueryConfig queryConfig, String schemaOrDatabase)
            throws SQLException {

        Map<String, TableMetadata> tablesMap = new HashMap<>();
        Map<String, List<ColumnMetadata>> columnsMap = new HashMap<>();

        ResultSet resultSet = executeTablesQuery(connection, queryConfig.getTablesAndColumnsQuery(),
                schemaOrDatabase, scan.getSourceConnector().getDatabaseType().name());

        while (resultSet.next()) {
            String tableName = resultSet.getString("table_name");

            TableMetadata table = getOrCreateTable(tablesMap, columnsMap, scan, tableName);
            ColumnMetadata column = buildColumnFromResultSet(resultSet, table,
                    scan.getSourceConnector().getDatabaseType().name());

            columnsMap.get(tableName).add(column);
        }
        resultSet.close();

        persistTablesAndColumns(tablesMap, columnsMap);

        return tablesMap;
    }

    private List<RelationMetadata> extractRelations(
            Connection connection, SystemScan scan, MetadataQueryConfig queryConfig, String schemaOrDatabase)
            throws SQLException {

        List<RelationMetadata> relations = new ArrayList<>();

        ResultSet resultSet = executeQuery(connection, queryConfig.getRelationshipsQuery(), schemaOrDatabase);

        while (resultSet.next()) {
            RelationMetadata relation = buildRelationFromResultSet(resultSet, scan);
            relations.add(relation);
        }
        resultSet.close();

        return relations;
    }

    private TableMetadata getOrCreateTable(
            Map<String, TableMetadata> tablesMap,
            Map<String, List<ColumnMetadata>> columnsMap,
            SystemScan scan,
            String tableName) {

        TableMetadata table = tablesMap.get(tableName);
        if (table == null) {
            table = TableMetadata.builder()
                    .systemScan(scan)
                    .tableName(tableName)
                    .columnMetadataList(new ArrayList<>())
                    .build();
            tablesMap.put(tableName, table);
            columnsMap.put(tableName, new ArrayList<>());
        }
        return table;
    }

    private ColumnMetadata buildColumnFromResultSet(ResultSet rs, TableMetadata table, String dbType) throws SQLException {
        String columnName = rs.getString("column_name");
        String dataType = rs.getString("data_type");
        String isNullable = rs.getString("is_nullable");
        Integer maxLength = rs.getObject("character_maximum_length") != null
                ? rs.getInt("character_maximum_length")
                : null;

        // Extract primary key info
        String columnKey = rs.getString("column_key");
        boolean isPrimaryKey = "PRI".equalsIgnoreCase(columnKey);

        // Extract auto-increment info (database-specific)
        boolean isAutoIncrement = false;
        if ("MYSQL".equalsIgnoreCase(dbType)) {
            String extra = rs.getString("extra");
            isAutoIncrement = extra != null && extra.toLowerCase().contains("auto_increment");
        } else if ("POSTGRESQL".equalsIgnoreCase(dbType)) {
            String columnDefault = rs.getString("column_default");
            isAutoIncrement = columnDefault != null &&
                    (columnDefault.startsWith("nextval") || dataType.equalsIgnoreCase("serial") ||
                            dataType.equalsIgnoreCase("bigserial"));
        } else if ("MSSQL".equalsIgnoreCase(dbType)) {
            int isIdentity = rs.getInt("is_identity");
            isAutoIncrement = isIdentity == 1;
        }

        return ColumnMetadata.builder()
                .table(table)
                .columnName(columnName)
                .dataType(dataType)
                .isNullable("YES".equalsIgnoreCase(isNullable))
                .length(maxLength)
                .isPrimaryKey(isPrimaryKey)
                .isAutoIncrement(isAutoIncrement)
                .build();
    }

    private RelationMetadata buildRelationFromResultSet(ResultSet rs, SystemScan scan) throws SQLException {
        return RelationMetadata.builder()
                .systemScan(scan)
                .foreignTable(rs.getString("foreign_table"))
                .foreignColumn(rs.getString("foreign_column"))
                .primaryTable(rs.getString("primary_table"))
                .primaryColumn(rs.getString("primary_column"))
                .build();
    }

    private void persistTablesAndColumns(
            Map<String, TableMetadata> tablesMap,
            Map<String, List<ColumnMetadata>> columnsMap) {

        tableRepository.saveAll(tablesMap.values());

        for (List<ColumnMetadata> columns : columnsMap.values()) {
            columnRepository.saveAll(columns);
        }
    }

    private ResultSet executeQuery(Connection connection, String query, String schemaOrDatabase) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement(query);
        stmt.setString(1, schemaOrDatabase);
        return stmt.executeQuery();
    }

    private ResultSet executeTablesQuery(Connection connection, String query, String schemaOrDatabase, String dbType)
            throws SQLException {
        PreparedStatement stmt = connection.prepareStatement(query);

        // PostgreSQL query requires schema parameter twice (for PK subquery and main query)
        if ("POSTGRESQL".equalsIgnoreCase(dbType)) {
            stmt.setString(1, schemaOrDatabase);
            stmt.setString(2, schemaOrDatabase);
        } else if ("MSSQL".equalsIgnoreCase(dbType)) {
            // MSSQL query requires schema parameter twice (for PK subquery and main query)
            stmt.setString(1, schemaOrDatabase);
            stmt.setString(2, schemaOrDatabase);
        } else {
            // MySQL only needs one parameter
            stmt.setString(1, schemaOrDatabase);
        }

        return stmt.executeQuery();
    }
}
