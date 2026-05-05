package com.db_migrator.etl_system.service.execution;

import com.db_migrator.etl_system.model.entity.transformation.*;
import com.db_migrator.etl_system.model.enums.ColumnTransformationType;
import com.db_migrator.etl_system.model.enums.DatabaseTypeEnum;
import com.db_migrator.etl_system.service.transformation.TransformationUtils;
import com.db_migrator.etl_system.service.transformation.TypeResolutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * DDL Generator Service
 * Generates CREATE TABLE DDL with FK constraints included
 * Uses DatabaseDialectLoader for extensible database-specific syntax
 * Features:
 * - CREATE TABLE with inline FK constraints
 * - AUTO_INCREMENT handling (SERIAL/AUTO_INCREMENT/IDENTITY)
 * - DEFAULT function mapping (CURRENT_TIMESTAMP/GETDATE(), UUID/NEWID)
 * - Identifier escaping (quotes/backticks/brackets)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DDLGenerator {

    private final TypeResolutionService typeResolutionService;
    private final DatabaseDialectLoader dialectLoader;

    public String generateCreateTableDDL(TransformationTable table, TransformationModel model, DatabaseTypeEnum targetDb) {
        String tableName = TransformationUtils.getEffectiveTableName(table);
        StringBuilder ddl = new StringBuilder();

        ddl.append("CREATE TABLE ").append(dialectLoader.escapeIdentifier(tableName, targetDb)).append(" (\n");

        List<String> columnDefs = new ArrayList<>();
        List<String> pkColumns = new ArrayList<>();

        // Generate column definitions
        for (TransformationColumn column : table.getColumns()) {
            if (TransformationUtils.isColumnDeleted(column)) {
                continue; // Skip excluded columns
            }

            String columnName = TransformationUtils.getEffectiveColumnName(column);
            String columnDef = generateColumnDefinition(column, targetDb);
            columnDefs.add(columnDef);

            // Track PK columns
            if (isPrimaryKey(column)) {
                pkColumns.add(dialectLoader.escapeIdentifier(columnName, targetDb));
            }
        }

        ddl.append(String.join(",\n", columnDefs));

        // Add PRIMARY KEY constraint
        if (!pkColumns.isEmpty()) {
            ddl.append(",\n  PRIMARY KEY (").append(String.join(", ", pkColumns)).append(")");
        }

        // Add FOREIGN KEY constraints (ONE-STEP: included in CREATE TABLE)
        List<String> fkConstraints = generateForeignKeyConstraints(tableName, model, targetDb);
        for (String fkConstraint : fkConstraints) {
            ddl.append(",\n  ").append(fkConstraint);
        }

        ddl.append("\n)");

        log.debug("Generated CREATE TABLE DDL for table {}: {} characters", tableName, ddl.length());

        return ddl.toString();
    }

    /**
     * Generate column definition with database-specific syntax
     */
    private String generateColumnDefinition(TransformationColumn column, DatabaseTypeEnum targetDb) {
        String columnName = TransformationUtils.getEffectiveColumnName(column);
        String dataType = typeResolutionService.getEffectiveColumnType(column);

        StringBuilder def = new StringBuilder();
        def.append("  ").append(dialectLoader.escapeIdentifier(columnName, targetDb));

        // Handle AUTO_INCREMENT using dialect configuration
        if (isAutoIncrement(column)) {
            def.append(" ").append(dialectLoader.getAutoIncrementDefinition(dataType, targetDb));
        } else {
            def.append(" ").append(dataType);
        }

        // NULL/NOT NULL
        if (!isNullable(column)) {
            def.append(" NOT NULL");
        }

        // DEFAULT value using dialect configuration
        String defaultValue = getDefaultValue(column, targetDb);
        if (defaultValue != null) {
            def.append(" DEFAULT ").append(defaultValue);
        }

        return def.toString();
    }

    /**
     * Check if column is auto-increment
     */
    private boolean isAutoIncrement(TransformationColumn column) {
        // Check if source column has auto-increment metadata
        if (column.getSourceColumnMetadata() != null) {
            return Boolean.TRUE.equals(column.getSourceColumnMetadata().getIsAutoIncrement());
        }
        return false;
    }

    /**
     * Get default value with database-specific function mapping
     */
    private String getDefaultValue(TransformationColumn column, DatabaseTypeEnum targetDb) {
        // Check for CHANGE_TYPE transformation with default value
        for (ColumnTransformationAssignment assignment : column.getAssignments()) {
            if (assignment.getTransformationType() == ColumnTransformationType.CHANGE_TYPE ||
                assignment.getTransformationType() == ColumnTransformationType.ADD_COLUMN) {

                String defaultValue = assignment.getDefaultValue();
                if (defaultValue != null && !defaultValue.isEmpty()) {
                    // Map using dialect configuration (CURRENT_TIMESTAMP, UUID, etc.)
                    return dialectLoader.mapDefaultFunction(defaultValue, targetDb);
                }
            }
        }

        return null;
    }

    /**
     * Generate FOREIGN KEY constraints for this table (included in CREATE TABLE)
     *
     * @param tableName Table name
     * @param model Transformation model
     * @param targetDb Target database type
     * @return List of FK constraint definitions (for inline CREATE TABLE)
     */
    private List<String> generateForeignKeyConstraints(String tableName, TransformationModel model, DatabaseTypeEnum targetDb) {
        List<String> fkConstraints = new ArrayList<>();

        for (TransformationRelation relation : model.getTransformationRelations()) {
            if (relation.getIsDeleted()) {
                continue;
            }

            // Only add FKs where this table is the foreign table
            if (!relation.getForeignTable().equals(tableName)) {
                continue;
            }

            String fkConstraint = String.format(
                "CONSTRAINT fk_%s_%s FOREIGN KEY (%s) REFERENCES %s(%s)",
                relation.getForeignTable(),
                relation.getForeignColumn(),
                dialectLoader.escapeIdentifier(relation.getForeignColumn(), targetDb),
                dialectLoader.escapeIdentifier(relation.getPrimaryTable(), targetDb),
                dialectLoader.escapeIdentifier(relation.getPrimaryColumn(), targetDb)
            );

            fkConstraints.add(fkConstraint);
        }

        return fkConstraints;
    }

    /**
     * Check if column is primary key
     */
    private boolean isPrimaryKey(TransformationColumn column) {
        // Check ADD_COLUMN assignment
        for (ColumnTransformationAssignment assignment : column.getAssignments()) {
            if (assignment.getTransformationType() == ColumnTransformationType.ADD_COLUMN) {
                return Boolean.TRUE.equals(assignment.getIsPrimaryKey());
            }
        }

        // Check source column metadata
        if (column.getSourceColumnMetadata() != null) {
            return Boolean.TRUE.equals(column.getSourceColumnMetadata().getIsPrimaryKey());
        }

        return false;
    }

    /**
     * Check if column is nullable
     */
    private boolean isNullable(TransformationColumn column) {
        // Check ADD_COLUMN assignment or source metadata
        for (ColumnTransformationAssignment assignment : column.getAssignments()) {
            if (assignment.getTransformationType() == ColumnTransformationType.ADD_COLUMN) {
                return Boolean.TRUE.equals(assignment.getIsNullable());
            }
        }

        // Fallback to source metadata
        if (column.getSourceColumnMetadata() != null) {
            return Boolean.TRUE.equals(column.getSourceColumnMetadata().getIsNullable());
        }

        return true; // Default to nullable
    }
}
