package com.database_migrator.domain.transformation.dto;

import com.database_migrator.domain.transformation.model.TableTransformationType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TableTransformationAssignmentRequest {

    @NotNull(message = "Transformation type is required")
    private TableTransformationType transformationType;

    private String newName;
}
