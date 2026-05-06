package com.database_migrator.domain.transformation.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TransformationColumnRequest {

    @NotNull(message = "Source column metadata ID is required")
    private Long sourceColumnMetadataId;

    private List<ColumnTransformationAssignmentRequest> columnTransformations;
}
