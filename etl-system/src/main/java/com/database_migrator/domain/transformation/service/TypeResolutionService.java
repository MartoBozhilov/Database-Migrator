package com.database_migrator.domain.transformation.service;

import com.database_migrator.config.migration.loaders.TypeMappingLoader;
import com.database_migrator.config.migration.models.TypeMapping;
import com.database_migrator.domain.transformation.model.ColumnTransformationAssignment;
import com.database_migrator.domain.transformation.model.TransformationColumn;
import com.database_migrator.domain.transformation.model.TransformationModel;
import com.database_migrator.domain.transformation.model.TransformationTable;
import com.database_migrator.domain.transformation.model.ColumnTransformationType;
import com.database_migrator.domain.connector.model.DatabaseTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TypeResolutionService {

    private final TypeMappingLoader typeMappingLoader;

    public void autoResolveAllColumnTypes(TransformationModel model) {
        DatabaseTypeEnum sourceDb = model.getSystemScan().getSourceConnector().getDatabaseType();
        DatabaseTypeEnum targetDb = model.getTargetConnector().getDatabaseType();

        boolean isCrossDatabase = sourceDb != targetDb;

        log.debug("Auto-resolving column types for model {} ({}→{})",
                model.getId(), sourceDb, targetDb);

        int resolvedCount = 0;

        for (TransformationTable table : model.getTransformationTables()) {
            for (TransformationColumn column : table.getColumns()) {
                String resolved = resolveColumnType(column, sourceDb, targetDb, isCrossDatabase);
                if (resolved != null) {
                    column.setResolvedTargetType(resolved);
                    resolvedCount++;
                }
            }
        }

        log.info("Resolved {} column types for model {}", resolvedCount, model.getId());
    }

    public String resolveAddColumnType(String dataType) {
        // For ADD_COLUMN, user explicitly specifies the type
        // Already verified by TypeMappingLoader -> isValidTargetType
        return dataType;
    }

    public String getEffectiveColumnType(TransformationColumn column) {
        if (column.getResolvedTargetType() != null) {
            return column.getResolvedTargetType();
        }

        // Fallback to source type (same-db, no transformation)
        if (column.getSourceColumnMetadata() != null) {
            return column.getSourceColumnMetadata().getDataType();
        }

        // ADD_COLUMN without resolved type (shouldn't happen)
        log.warn("Column {} has no resolved type and no source metadata", column.getId());
        return "VARCHAR(255)"; // Emergency fallback
    }

    private String resolveColumnType(TransformationColumn column,
                                     DatabaseTypeEnum sourceDb,
                                     DatabaseTypeEnum targetDb,
                                     boolean isCrossDatabase) {

        // ADD_COLUMN: resolvedTargetType already set when column created
        if (column.getSourceColumnMetadata() == null) {
            // Column should already have resolvedTargetType from ADD_COLUMN assignment
            Optional<ColumnTransformationAssignment> addColumn = column.getAssignments()
                    .stream()
                    .filter(a -> a.getTransformationType() == ColumnTransformationType.ADD_COLUMN)
                    .findFirst();

            if (addColumn.isPresent()) {
                return addColumn.get().getDataType();
            }

            log.warn("ADD_COLUMN without dataType for column {}", column.getId());
            return null;
        }

        String sourceType = column.getSourceColumnMetadata().getDataType();

        // Check if user applied CHANGE_TYPE transformation
        Optional<ColumnTransformationAssignment> changeType = column.getAssignments()
                .stream()
                .filter(a -> a.getTransformationType() == ColumnTransformationType.CHANGE_TYPE)
                .findFirst();

        // If CHANGE_TYPE exists, use user's specified type (regardless of cross-db or same-db)
        if (changeType.isPresent()) {
            return changeType.get().getTargetDataType();
        }

        // Cross-database: ALWAYS resolve to best target type
        if (isCrossDatabase) {
            return resolveCrossDatabaseType(sourceType, sourceDb, targetDb);
        }

        // Same-database, no CHANGE_TYPE: leave NULL
        return null;
    }

    /**
     * Resolve cross-database type conversion.
     * Picks the best (safest) target type from allowed options.
     */
    private String resolveCrossDatabaseType(String sourceType,
                                            DatabaseTypeEnum sourceDb,
                                            DatabaseTypeEnum targetDb) {
        try {
            List<TypeMapping> allowedTypes = typeMappingLoader
                    .getAllowedTargetTypes(sourceType, sourceDb, targetDb);

            if (allowedTypes.isEmpty()) {
                log.warn("No type mapping found for {} {} → {}, keeping source type",
                        sourceDb, sourceType, targetDb);
                return sourceType; // Fallback: keep source type
            }

            // Strategy: Pick first non-risky option, or first option if all risky
            TypeMapping bestMapping = allowedTypes.stream()
                    .filter(m -> !m.isDataLossRisk())
                    .findFirst()
                    .orElse(allowedTypes.getFirst());

            log.debug("Resolved {} {} → {} {}",
                    sourceDb, sourceType, targetDb, bestMapping.getTargetType());

            return bestMapping.getTargetType();

        } catch (Exception e) {
            log.error("Failed to resolve type {} {} → {}", sourceDb, sourceType, targetDb, e);
            return sourceType; // Fallback: keep source type
        }
    }
}
