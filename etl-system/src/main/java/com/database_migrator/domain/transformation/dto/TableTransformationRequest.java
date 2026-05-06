package com.database_migrator.domain.transformation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TableTransformationRequest {

    // For RENAME_TABLE
    private String newName;

    // For ADD_TABLE
    private String tableName;

    private String idGenerationStrategy; // AUTO_INCREMENT, UUID, SEQUENCE

    private String idColumnName; // User-specified ID column name (e.g., "id", "user_id", "audit_id")

    private String idColumnDataType; // User-specified data type (e.g., "BIGINT", "VARCHAR(36)", "INT")
}
