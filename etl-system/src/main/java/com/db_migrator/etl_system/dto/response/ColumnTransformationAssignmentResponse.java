package com.db_migrator.etl_system.dto.response;

import com.db_migrator.etl_system.model.enums.ColumnTransformationType;
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

    // For CHANGE_TYPE
    private String targetDataType;
}
