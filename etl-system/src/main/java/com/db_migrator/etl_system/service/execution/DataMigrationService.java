package com.db_migrator.etl_system.service.execution;

import com.db_migrator.etl_system.dto.helper.ColumnMapping;
import com.db_migrator.etl_system.model.entity.execution.Task;
import com.db_migrator.etl_system.model.entity.transformation.*;
import com.db_migrator.etl_system.model.enums.ColumnTransformationType;
import com.db_migrator.etl_system.model.enums.DatabaseTypeEnum;
import com.db_migrator.etl_system.service.transformation.TransformationUtils;
import com.db_migrator.etl_system.service.transformation.TypeResolutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Data Migration Service
 *
 * Copies data from source to target with transformations in batches
 * Uses DatabaseDialectLoader for extensible database-specific syntax
 *
 * Features:
 * - Batched data copy (1000 rows per batch)
 * - Database-specific SELECT pagination
 * - Column transformations (expressions, defaults)
 * - Progress tracking
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DataMigrationService {

    private static final int BATCH_SIZE = 1000; // Rows per batch

    private final TypeResolutionService typeResolutionService;
    private final DatabaseDialectLoader dialectLoader;

    /**
     * Migrate data from source to target table in batches
     *
     * @param task Task being executed
     * @param sourceConn Source database connection
     * @param targetConn Target database connection
     * @param table Transformation table
     * @return Total rows migrated
     */
    public long migrateTableData(Task task, Connection sourceConn, Connection targetConn,
                                 TransformationTable table) throws SQLException {

        DatabaseTypeEnum sourceDb = task.getCycle().getTransformationModel().getSystemScan()
            .getSourceConnector().getDatabaseType();
        DatabaseTypeEnum targetDb = task.getCycle().getTargetConnector().getDatabaseType();

        String sourceTableName = table.getSourceTableMetadata().getTableName();
        String targetTableName = TransformationUtils.getEffectiveTableName(table);

        long totalRows = 0;
        int offset = 0;

        // Build column mappings (source -> target with transformations)
        List<ColumnMapping> columnMappings = buildColumnMappings(table);

        if (columnMappings.isEmpty()) {
            log.warn("Task {}: No columns to migrate for table {}", task.getId(), targetTableName);
            return 0;
        }

        // Prepare INSERT statement
        String insertSQL = buildInsertStatement(targetTableName, columnMappings, targetDb);

        try (PreparedStatement insertStmt = targetConn.prepareStatement(insertSQL)) {

            while (true) {
                // Read batch from source (database-specific pagination)
                String selectSQL = buildSelectStatement(sourceTableName, columnMappings, sourceDb, offset, BATCH_SIZE);

                List<Map<String, Object>> batch = new ArrayList<>();

                try (PreparedStatement selectStmt = sourceConn.prepareStatement(selectSQL);
                     ResultSet rs = selectStmt.executeQuery()) {

                    while (rs.next()) {
                        Map<String, Object> row = new HashMap<>();
                        for (ColumnMapping mapping : columnMappings) {
                            row.put(mapping.getSourceColumn(), rs.getObject(mapping.getSourceColumn()));
                        }
                        batch.add(row);
                    }
                }

                // No more data
                if (batch.isEmpty()) {
                    break;
                }

                // Apply transformations and insert batch
                for (Map<String, Object> row : batch) {
                    applyTransformationsAndInsert(row, columnMappings, insertStmt);
                    insertStmt.addBatch();
                }

                // Execute batch
                int[] results = insertStmt.executeBatch();
                targetConn.commit();

                totalRows += batch.size();
                task.setRowsProcessed(totalRows);

                log.info("Task {}: Migrated {} rows (total: {})",
                    task.getId(), batch.size(), totalRows);

                // Move to next batch
                offset += BATCH_SIZE;

                // If batch was smaller than BATCH_SIZE, we're done
                if (batch.size() < BATCH_SIZE) {
                    break;
                }
            }
        }

        log.info("Task {}: Migration completed. Total rows: {}", task.getId(), totalRows);
        return totalRows;
    }

    /**
     * Build SELECT statement with database-specific pagination (via dialect configuration)
     */
    private String buildSelectStatement(String tableName, List<ColumnMapping> columnMappings,
                                       DatabaseTypeEnum sourceDb, int offset, int limit) {

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");

        // Column list (all source columns)
        List<String> sourceColumns = columnMappings.stream()
            .map(m -> dialectLoader.escapeIdentifier(m.getSourceColumn(), sourceDb))
            .collect(Collectors.toList());

        if (sourceColumns.isEmpty()) {
            throw new RuntimeException("No source columns to select");
        }

        sql.append(String.join(", ", sourceColumns));
        sql.append(" FROM ").append(dialectLoader.escapeIdentifier(tableName, sourceDb));

        // Get primary key column for ORDER BY (if required by dialect)
        String pkColumn = null;
        if (dialectLoader.requiresOrderByForPagination(sourceDb)) {
            pkColumn = columnMappings.stream()
                .filter(ColumnMapping::isPrimaryKey)
                .map(ColumnMapping::getSourceColumn)
                .findFirst()
                .orElse(columnMappings.get(0).getSourceColumn()); // Fallback to first column
        }

        // Build pagination clause using dialect configuration
        String paginationClause = dialectLoader.buildPaginationClause(sourceDb, offset, limit, pkColumn);
        sql.append(" ").append(paginationClause);

        return sql.toString();
    }

    /**
     * Build INSERT statement for target
     */
    private String buildInsertStatement(String tableName, List<ColumnMapping> columnMappings,
                                       DatabaseTypeEnum targetDb) {

        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ").append(dialectLoader.escapeIdentifier(tableName, targetDb));
        sql.append(" (");

        // Target column names
        sql.append(columnMappings.stream()
            .map(m -> dialectLoader.escapeIdentifier(m.getTargetColumn(), targetDb))
            .collect(Collectors.joining(", ")));

        sql.append(") VALUES (");

        // Placeholders
        sql.append(columnMappings.stream()
            .map(m -> "?")
            .collect(Collectors.joining(", ")));

        sql.append(")");

        return sql.toString();
    }

    /**
     * Build column mappings from transformation table
     */
    private List<ColumnMapping> buildColumnMappings(TransformationTable table) {
        List<ColumnMapping> mappings = new ArrayList<>();

        for (TransformationColumn column : table.getColumns()) {
            if (TransformationUtils.isColumnDeleted(column)) {
                continue; // Skip excluded columns
            }

            ColumnMapping mapping = new ColumnMapping();
            mapping.setSourceColumn(column.getSourceColumnMetadata().getColumnName());
            mapping.setTargetColumn(TransformationUtils.getEffectiveColumnName(column));
            mapping.setSourceType(column.getSourceColumnMetadata().getDataType());
            mapping.setTargetType(typeResolutionService.getEffectiveColumnType(column));

            // Get primary key info from metadata
            boolean isPrimaryKey = column.getSourceColumnMetadata() != null &&
                    Boolean.TRUE.equals(column.getSourceColumnMetadata().getIsPrimaryKey());
            mapping.setPrimaryKey(isPrimaryKey);

            // Check for default value
            String defaultValue = getDefaultValue(column);
            mapping.setDefaultValue(defaultValue);

            mappings.add(mapping);
        }

        return mappings;
    }

    /**
     * Get default value from column assignments
     */
    private String getDefaultValue(TransformationColumn column) {
        for (ColumnTransformationAssignment assignment : column.getAssignments()) {
            if (assignment.getTransformationType() == ColumnTransformationType.CHANGE_TYPE ||
                assignment.getTransformationType() == ColumnTransformationType.ADD_COLUMN) {

                String defaultValue = assignment.getDefaultValue();
                if (defaultValue != null && !defaultValue.isEmpty()) {
                    return defaultValue;
                }
            }
        }
        return null;
    }

    /**
     * Apply transformations to row and set INSERT statement parameters
     */
    private void applyTransformationsAndInsert(Map<String, Object> row,
                                              List<ColumnMapping> columnMappings,
                                              PreparedStatement insertStmt) throws SQLException {

        for (int i = 0; i < columnMappings.size(); i++) {
            ColumnMapping mapping = columnMappings.get(i);

            // Direct mapping from source
            Object value = row.get(mapping.getSourceColumn());

            // Use default value if source is NULL
            if (value == null && mapping.getDefaultValue() != null) {
                value = mapping.getDefaultValue();
            }

            // Type conversion if needed (JDBC handles most conversions automatically)
            insertStmt.setObject(i + 1, value);
        }
    }
}
