package com.database_migrator.domain.execution.service;

import com.database_migrator.domain.transformation.dto.ValidationResult;
import com.database_migrator.domain.connector.model.Connector;
import com.database_migrator.domain.execution.model.Cycle;
import com.database_migrator.domain.scan.model.SystemScan;
import com.database_migrator.domain.transformation.model.TransformationModel;
import com.database_migrator.domain.transformation.model.TransformationTable;
import com.database_migrator.domain.connector.model.DatabaseTypeEnum;
import com.database_migrator.domain.scan.model.ScanStatusEnum;
import com.database_migrator.config.database.MetadataQueryLoader;
import com.database_migrator.domain.common.util.TransformationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Pre-execution validator for cycles
 * Validates before execution starts:
 * 1. DAG is acyclic (no circular dependencies)
 * 2. Source database connection is valid
 * 3. Target database connection is valid
 * 4. Target database schema is empty (no existing tables)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PreExecutionValidator {

    private final DAGBuilder dagBuilder;
    private final MetadataQueryLoader metadataQueryLoader;

    /**
     * Validate cycle readiness before execution
     */
    public ValidationResult validateCycle(Cycle cycle) {
        ValidationResult result = new ValidationResult(true);

        TransformationModel model = cycle.getTransformationModel();
        SystemScan scan = model.getSystemScan();
        Connector sourceConnector = cycle.getSourceConnector();
        Connector targetConnector = cycle.getTargetConnector();

        // Validate transformation model is from completed scan
        if (scan.getStatus() != ScanStatusEnum.COMPLETED) {
            result.addError("System scan must be in COMPLETED status (current: " + scan.getStatus() + ")");
        }

        // Validate DAG is acyclic
        ValidationResult dagValidation = validateDAGAcyclic(model);
        if (dagValidation.hasErrors()) {
            result.addError("DAG validation failed: " + dagValidation.getErrorMessages());
        }

        // Validate source connection
        ValidationResult sourceValidation = validateConnection(sourceConnector, "Source");
        if (sourceValidation.hasErrors()) {
            result.addError("Source connection failed: " + sourceValidation.getErrorMessages());
        }

        // Validate target connection
        ValidationResult targetValidation = validateConnection(targetConnector, "Target");
        if (targetValidation.hasErrors()) {
            result.addError("Target connection failed: " + targetValidation.getErrorMessages());
        }

        // Validate target schema is empty (only if target connection is valid)
        if (!targetValidation.hasErrors()) {
            ValidationResult schemaValidation = validateTargetSchemaEmpty(targetConnector, model);
            if (schemaValidation.hasErrors()) {
                result.addError("Target schema validation failed: " + schemaValidation.getErrorMessages());
            }
        }

        return result;
    }

    public ValidationResult validateDAGAcyclic(TransformationModel model) {
        ValidationResult result = new ValidationResult(true);

        try {
            // Build dependency graph
            Map<String, Set<String>> graph = dagBuilder.buildDependencyGraph(model);

            // Check for cycles
            if (!dagBuilder.hasNoCycles(graph)) {
                result.addError("Transformation model contains circular dependencies. " +
                        "Cannot create DAG for execution. " +
                        "Please review foreign key relationships and exclude tables/relations to break cycles.");
            }

        } catch (Exception e) {
            result.addError("Failed to build dependency graph: " + e.getMessage());
        }

        return result;
    }

    public ValidationResult validateConnection(Connector connector, String label) {
        ValidationResult result = new ValidationResult(true);

        try {
            String jdbcUrl = buildJdbcUrl(connector);

            try (Connection conn = DriverManager.getConnection(
                    jdbcUrl,
                    connector.getUsername(),
                    connector.getPassword())) {

                // Connection successful
                log.info("{} database connection successful: {}", label, connector.getName());

            }
        } catch (Exception e) {
            result.addError(label + " database connection failed: " + e.getMessage());
            log.error("{} database connection failed for connector {}: {}",
                    label, connector.getName(), e.getMessage());
        }

        return result;
    }

    public ValidationResult validateTargetSchemaEmpty(Connector targetConnector, TransformationModel model) {
        ValidationResult result = new ValidationResult(true);

        try {
            String jdbcUrl = buildJdbcUrl(targetConnector);

            try (Connection conn = DriverManager.getConnection(
                    jdbcUrl,
                    targetConnector.getUsername(),
                    targetConnector.getPassword())) {

                // Get list of tables that will be created
                Set<String> tablesToCreate = getIncludedTableNames(model);

                // Check if any of these tables already exist in target
                List<String> existingTables = new ArrayList<>();

                for (String tableName : tablesToCreate) {
                    if (tableExists(conn, tableName, targetConnector.getDatabaseType())) {
                        existingTables.add(tableName);
                    }
                }

                if (!existingTables.isEmpty()) {
                    result.addError("Target database already contains " + existingTables.size() +
                            " table(s) that will be created: " + String.join(", ", existingTables) +
                            ". Target database must be empty before migration.");
                }

            }
        } catch (Exception e) {
            result.addError("Failed to check target schema: " + e.getMessage());
            log.error("Failed to validate target schema for connector {}: {}",
                    targetConnector.getName(), e.getMessage());
        }

        return result;
    }

    private String buildJdbcUrl(Connector connector) {
        return String.format("jdbc:%s://%s:%d/%s",
                connector.getDatabaseType().name().toLowerCase(),
                connector.getHost(),
                connector.getPort(),
                connector.getDatabaseName());
    }

    private boolean tableExists(Connection conn, String tableName, DatabaseTypeEnum dbType) {
        try {
            // Get database-specific table existence query
            String query = metadataQueryLoader.getQueryConfig(dbType).getTableExistsQuery();

            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, tableName);

                try (ResultSet rs = stmt.executeQuery()) {
                    boolean exists = rs.next();
                    if (exists) {
                        log.debug("Table '{}' found in target database", tableName);
                    }
                    return exists;
                }
            }

        } catch (SQLException e) {
            log.error("Error checking if table '{}' exists in {} database: {}",
                    tableName, dbType, e.getMessage());
            return false;
        }
    }

    private Set<String> getIncludedTableNames(TransformationModel model) {
        Set<String> tableNames = new HashSet<>();

        for (TransformationTable table : model.getTransformationTables()) {
            if (!TransformationUtils.isTableDeleted(table)) {
                String tableName = TransformationUtils.getEffectiveTableName(table);
                if (tableName != null) {
                    tableNames.add(tableName);
                }
            }
        }

        return tableNames;
    }
}
