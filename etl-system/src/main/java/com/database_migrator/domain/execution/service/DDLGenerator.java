package com.database_migrator.domain.execution.service;

import com.database_migrator.config.migration.loaders.DatabaseDialectLoader;
import com.database_migrator.domain.transformation.model.ColumnTransformationAssignment;
import com.database_migrator.domain.transformation.model.TransformationColumn;
import com.database_migrator.domain.transformation.model.TransformationModel;
import com.database_migrator.domain.transformation.model.TransformationRelation;
import com.database_migrator.domain.transformation.model.TransformationTable;
import com.database_migrator.domain.transformation.model.ColumnTransformationType;
import com.database_migrator.domain.connector.model.DatabaseTypeEnum;
import com.database_migrator.domain.common.util.TransformationUtils;
import com.database_migrator.domain.transformation.service.TypeResolutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates CREATE TABLE DDL with FK constraints included
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DDLGenerator {

    private final TypeResolutionService typeResolutionService;
    private final DatabaseDialectLoader dialectLoader;
    private final EnumTypeHandler enumTypeHandler;

    public String generateCreateTableDDL(TransformationTable table, TransformationModel model, DatabaseTypeEnum targetDb) {
        String tableName = TransformationUtils.getEffectiveTableName(table);
        StringBuilder ddl = new StringBuilder();

        // For PostgreSQL: Generate CREATE TYPE statements for ENUM columns first
        if (targetDb == DatabaseTypeEnum.POSTGRESQL) {
            List<String> enumTypeStatements = generateEnumTypeStatements(table, tableName, targetDb);
            for (String enumTypeStmt : enumTypeStatements) {
                ddl.append(enumTypeStmt).append(";\n");
            }
            if (!enumTypeStatements.isEmpty()) {
                ddl.append("\n"); // Blank line before CREATE TABLE
            }
        }

        ddl.append("CREATE TABLE ");
        ddl.append(dialectLoader.escapeIdentifier(tableName, targetDb)).append(" (\n");

        List<String> columnDefs = new ArrayList<>();
        List<String> pkColumns = new ArrayList<>();

        // Generate column definitions
        for (TransformationColumn column : table.getColumns()) {
            if (TransformationUtils.isColumnDeleted(column)) {
                continue; // Skip excluded columns
            }

            String columnName = TransformationUtils.getEffectiveColumnName(column);
            String columnDef = generateColumnDefinition(column, tableName, targetDb);
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

        // Add FOREIGN KEY constraints
        List<String> fkConstraints = generateForeignKeyConstraints(tableName, model, targetDb);
        for (String fkConstraint : fkConstraints) {
            ddl.append(",\n  ").append(fkConstraint);
        }

        ddl.append("\n)");

        log.debug("Generated CREATE TABLE DDL for table {}: {} characters", tableName, ddl.length());
        log.debug("Generated DDL for table {}:\n{}", tableName, ddl);

        return ddl.toString();
    }

    /**
     * Generate column definition with database-specific syntax
     */
    private String generateColumnDefinition(TransformationColumn column, String tableName, DatabaseTypeEnum targetDb) {
        String columnName = TransformationUtils.getEffectiveColumnName(column);
        String dataType = typeResolutionService.getEffectiveColumnType(column);

        StringBuilder def = new StringBuilder();
        def.append("  ").append(dialectLoader.escapeIdentifier(columnName, targetDb));

        // Handle AUTO_INCREMENT using dialect configuration
        if (isAutoIncrement(column)) {
            def.append(" ").append(dialectLoader.getAutoIncrementDefinition(dataType, targetDb));
        } else {
            // For PostgreSQL: Convert ENUM types to custom type names
            String finalType = dataType;
            if (targetDb == DatabaseTypeEnum.POSTGRESQL && enumTypeHandler.isEnumType(dataType)) {
                String enumTypeName = enumTypeHandler.generateEnumTypeName(tableName, columnName);
                finalType = enumTypeName;
                log.debug("Using custom ENUM type {} for column {}.{}", enumTypeName, tableName, columnName);
            }
            def.append(" ").append(finalType);
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
     * Generate CREATE TYPE statements for all ENUM columns in a table
     */
    private List<String> generateEnumTypeStatements(TransformationTable table, String tableName, DatabaseTypeEnum targetDb) {
        List<String> statements = new ArrayList<>();

        for (TransformationColumn column : table.getColumns()) {
            if (TransformationUtils.isColumnDeleted(column)) {
                continue;
            }

            String dataType = typeResolutionService.getEffectiveColumnType(column);

            // Check if this is an ENUM type
            if (enumTypeHandler.isEnumType(dataType)) {
                String columnName = TransformationUtils.getEffectiveColumnName(column);
                List<String> enumValues = enumTypeHandler.extractEnumValues(dataType);

                if (!enumValues.isEmpty()) {
                    String typeName = enumTypeHandler.generateEnumTypeName(tableName, columnName);

                    // Generate DROP TYPE IF EXISTS first (for safety on re-run)
                    statements.add(enumTypeHandler.generateDropTypeStatement(typeName));

                    // Generate CREATE TYPE
                    String createTypeStmt = enumTypeHandler.generateCreateTypeStatement(typeName, enumValues);
                    if (createTypeStmt != null) {
                        statements.add(createTypeStmt);
                        log.info("Generated ENUM type {} with {} values for {}.{}",
                                typeName, enumValues.size(), tableName, columnName);
                    }
                }
            }
        }

        return statements;
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
