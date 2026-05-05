package com.db_migrator.etl_system.service.transformation;

import com.db_migrator.etl_system.config.TypeMapping;
import com.db_migrator.etl_system.model.entity.transformation.ColumnTransformationAssignment;
import com.db_migrator.etl_system.model.entity.transformation.TransformationColumn;
import com.db_migrator.etl_system.model.entity.transformation.TransformationModel;
import com.db_migrator.etl_system.model.entity.transformation.TransformationTable;
import com.db_migrator.etl_system.model.enums.ColumnTransformationType;
import com.db_migrator.etl_system.model.enums.DatabaseTypeEnum;
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

    /**
     * Auto-resolve target types for all columns in a transformation model.
     * <p>
     * Resolution rules:
     * 1. Cross-database migration (MySQL→PostgreSQL): ALWAYS resolve, even without transformations
     * 2. Same-database + CHANGE_TYPE: resolve to user's specified type
     * 3. Same-database + no transformations: leave NULL (use source type in Phase 5)
     * 4. ADD_COLUMN: resolvedTargetType already set when column created
     */
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

    /**
     * Resolve target type for a single column.
     *
     * @return resolved type or NULL if no resolution needed (same-db, no transformation)
     */
    public String resolveColumnType(TransformationColumn column,
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

        // Same-database, no CHANGE_TYPE: leave NULL (use source type in Phase 5)
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

    /**
     * Resolve type for a newly added column (ADD_COLUMN transformation).
     * This is called when user creates ADD_COLUMN assignment.
     */
    public String resolveAddColumnType(String dataType) {
        // For ADD_COLUMN, user explicitly specifies the type
        // Just return it as-is (validation should happen before calling this)
        return dataType;
    }

    /**
     * Get effective column type for Phase 5 execution.
     * If resolvedTargetType exists, use it. Otherwise use source type.
     */
    public String getEffectiveColumnType(TransformationColumn column) {
        if (column.getResolvedTargetType() != null) {
            return column.getResolvedTargetType();
        }

        // Fallback to source type (same-db, no transformation)
        if (column.getSourceColumnMetadata() != null) {
            String dataType = column.getSourceColumnMetadata().getDataType();

            // Quick fix: If VARCHAR/CHAR without length, add default length
            if (dataType.equalsIgnoreCase("varchar") || dataType.equalsIgnoreCase("char")) {
                return dataType + "(255)";
            }
            if (dataType.toLowerCase().startsWith("varchar") && !dataType.contains("(")) {
                return "varchar(255)";
            }
            if (dataType.toLowerCase().startsWith("char") && !dataType.contains("(")) {
                return "char(255)";
            }

            return dataType;
        }

        // ADD_COLUMN without resolved type (shouldn't happen)
        log.warn("Column {} has no resolved type and no source metadata", column.getId());
        return "VARCHAR(255)"; // Emergency fallback
    }
}
