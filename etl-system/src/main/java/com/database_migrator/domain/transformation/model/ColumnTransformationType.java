package com.database_migrator.domain.transformation.model;

public enum ColumnTransformationType {
    RENAME_COLUMN,    // Change column name
    CHANGE_TYPE,      // Change data type
    ADD_COLUMN,       // User creates new column (not in scan)
    DELETE_COLUMN     // Soft delete column (exclude from migration)
}
