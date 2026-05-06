package com.database_migrator.domain.execution.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Column mapping for data migration (source -> target with transformations)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ColumnMapping {

    private String sourceColumn;
    private String targetColumn;
    private String sourceType;
    private String targetType;
    private boolean isPrimaryKey;
    private String defaultValue;
}
