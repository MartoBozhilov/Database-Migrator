package com.db_migrator.etl_system.model.enums;

public enum TableTransformationType {
    RENAME_TABLE,     // Change table name
    ADD_TABLE,        // User creates new table (not in scan)
    DELETE_TABLE      // Soft delete table (exclude from migration)
}
