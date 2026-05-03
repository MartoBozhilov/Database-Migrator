package com.db_migrator.etl_system.dto.request;

import com.db_migrator.etl_system.model.enums.ColumnTransformationType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ColumnTransformationAssignmentRequest {

    @NotNull(message = "Transformation type is required")
    private ColumnTransformationType transformationType;

    private String newName;
    private String targetDataType;
    private String expression;
    private String defaultValue;
}
