package com.database_migrator.domain.transformation.dto;

import com.database_migrator.domain.transformation.model.ColumnTransformationType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ColumnTransformationAssignmentResponse {
    private Long id;
    private ColumnTransformationType transformationType;

    // For RENAME_COLUMN
    private String newName;

    // For ADD_COLUMN
    private String columnName;
    private String dataType;
    private Boolean isNullable;
    private Boolean isPrimaryKey;
    private String defaultValue;

    // For CHANGE_TYPE
    private String targetDataType;
}
