package com.db_migrator.etl_system.service.metadata;

import com.db_migrator.etl_system.config.MetadataQueryConfig;
import com.db_migrator.etl_system.model.entity.connector.Connector;
import com.db_migrator.etl_system.model.entity.metadata.ColumnMetadata;
import com.db_migrator.etl_system.model.entity.metadata.RelationMetadata;
import com.db_migrator.etl_system.model.entity.metadata.SystemScan;
import com.db_migrator.etl_system.model.entity.metadata.TableMetadata;
import com.db_migrator.etl_system.model.enums.ScanStatusEnum;
import com.db_migrator.etl_system.repository.ColumnMetadataRepository;
import com.db_migrator.etl_system.repository.RelationMetadataRepository;
import com.db_migrator.etl_system.repository.SystemScanRepository;
import com.db_migrator.etl_system.repository.TableMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.sql.DriverManager;
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

    @Async("taskExecutor")
    @Transactional
    public void extractMetadataAsync(Long scanId) {
        log.info("Starting metadata extraction for scan ID: {}", scanId);

        try {
            SystemScan scan = scanRepository.findById(scanId)
                    .orElseThrow(() -> new RuntimeException("Scan not found with ID: " + scanId));

            Connector connector = scan.getSourceConnector();
            String jdbcUrl = buildJdbcUrl(connector);
            String schemaOrDatabase = getSchemaOrDatabase(connector);

            updateScanStatus(scanId, ScanStatusEnum.RUNNING, new Date(), null, null);

            try (Connection connection = DriverManager.getConnection(
                    jdbcUrl, connector.getUsername(), connector.getPassword())) {

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
                throw new RuntimeException("Metadata extraction failed", e);
            }

        } catch (Exception e) {
            log.error("Unexpected error during metadata extraction for scan ID: {}", scanId, e);
            updateScanStatus(scanId, ScanStatusEnum.FAILED, null, new Date(), e.getMessage());
        }
    }

    public Map<String, String> testConnection(Connector connector) {
        Map<String, String> result = new HashMap<>();
        try {
            String jdbcUrl = buildJdbcUrl(connector);
            Connection connection = DriverManager.getConnection(
                    jdbcUrl,
                    connector.getUsername(),
                    connector.getPassword()
            );

            java.sql.DatabaseMetaData metaData = connection.getMetaData();
            String databaseVersion = metaData.getDatabaseProductName() + " " + metaData.getDatabaseProductVersion();

            connection.close();

            result.put("success", "true");
            result.put("message", "Connection successful");
            result.put("databaseVersion", databaseVersion);
        } catch (Exception e) {
            result.put("success", "false");
            result.put("message", "Connection failed: " + e.getMessage());
        }
        return result;
    }

    @Transactional
    public void updateScanStatus(Long scanId, ScanStatusEnum status, Date startedAt, Date completedAt,
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

    private String buildJdbcUrl(Connector connector) {
        return switch (connector.getDatabaseType()) {
            case MYSQL -> String.format("jdbc:mysql://%s:%d/%s",
                    connector.getHost(), connector.getPort(), connector.getDatabaseName());
            case POSTGRESQL -> String.format("jdbc:postgresql://%s:%d/%s",
                    connector.getHost(), connector.getPort(), connector.getDatabaseName());
            case MSSQL -> String.format("jdbc:sqlserver://%s:%d;databaseName=%s",
                    connector.getHost(), connector.getPort(), connector.getDatabaseName());
        };
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

        ResultSet resultSet = executeQuery(connection, queryConfig.getTablesAndColumnsQuery(), schemaOrDatabase);

        while (resultSet.next()) {
            String tableName = resultSet.getString("table_name");

            TableMetadata table = getOrCreateTable(tablesMap, columnsMap, scan, tableName);
            ColumnMetadata column = buildColumnFromResultSet(resultSet, table);

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

    private ColumnMetadata buildColumnFromResultSet(ResultSet rs, TableMetadata table) throws SQLException {
        String columnName = rs.getString("column_name");
        String dataType = rs.getString("data_type");
        String isNullable = rs.getString("is_nullable");
        Integer maxLength = rs.getObject("character_maximum_length") != null
                ? rs.getInt("character_maximum_length")
                : null;

        return ColumnMetadata.builder()
                .table(table)
                .columnName(columnName)
                .dataType(dataType)
                .isNullable("YES".equalsIgnoreCase(isNullable))
                .length(maxLength)
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
}
