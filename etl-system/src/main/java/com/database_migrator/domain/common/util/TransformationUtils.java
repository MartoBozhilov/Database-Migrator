package com.database_migrator.domain.common.util;

import com.database_migrator.domain.transformation.model.ColumnTransformationAssignment;
import com.database_migrator.domain.transformation.model.TableTransformationAssignment;
import com.database_migrator.domain.transformation.model.TransformationColumn;
import com.database_migrator.domain.transformation.model.TransformationModel;
import com.database_migrator.domain.transformation.model.TransformationTable;
import com.database_migrator.domain.transformation.model.ColumnTransformationType;
import com.database_migrator.domain.transformation.model.TableTransformationType;

import java.util.Optional;

public final class TransformationUtils {

    private TransformationUtils() {
        // Prevent instantiation
    }

    public static void validateModelNotConfirmed(TransformationModel model) {
        if (model.getIsConfirmed()) {
            throw new RuntimeException("Cannot modify confirmed transformation model. Model is immutable after confirmation.");
        }
    }

    public static String getEffectiveTableName(TransformationTable table) {
        // Check for RENAME_TABLE
        Optional<String> renamedName = table.getAssignments().stream()
                .filter(a -> a.getTransformationType() == TableTransformationType.RENAME_TABLE)
                .map(TableTransformationAssignment::getNewName)
                .findFirst();

        if (renamedName.isPresent()) {
            return renamedName.get();
        }

        // Check for ADD_TABLE
        Optional<String> addedName = table.getAssignments().stream()
                .filter(a -> a.getTransformationType() == TableTransformationType.ADD_TABLE)
                .map(TableTransformationAssignment::getTableName)
                .findFirst();

        return addedName.orElseGet(() -> table.getSourceTableMetadata() != null
                ? table.getSourceTableMetadata().getTableName()
                : null);
    }

    public static String getEffectiveColumnName(TransformationColumn column) {
        // Check for RENAME_COLUMN
        Optional<String> renamedName = column.getAssignments().stream()
                .filter(a -> a.getTransformationType() == ColumnTransformationType.RENAME_COLUMN)
                .map(ColumnTransformationAssignment::getNewName)
                .findFirst();

        if (renamedName.isPresent()) {
            return renamedName.get();
        }

        // Check for ADD_COLUMN
        Optional<String> addedName = column.getAssignments().stream()
                .filter(a -> a.getTransformationType() == ColumnTransformationType.ADD_COLUMN)
                .map(ColumnTransformationAssignment::getColumnName)
                .findFirst();

        return addedName.orElseGet(() -> column.getSourceColumnMetadata() != null
                ? column.getSourceColumnMetadata().getColumnName()
                : null);
    }

    public static boolean isTableDeleted(TransformationTable table) {
        return table.getAssignments().stream()
                .anyMatch(a -> a.getTransformationType() == TableTransformationType.DELETE_TABLE);
    }

    public static boolean isColumnDeleted(TransformationColumn column) {
        return column.getAssignments().stream()
                .anyMatch(a -> a.getTransformationType() == ColumnTransformationType.DELETE_COLUMN);
    }
}
