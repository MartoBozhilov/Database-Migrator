package com.database_migrator.domain.transformation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TransformationColumnResponse {
    private Long id;
    private String sourceColumnName;
    private String sourceDataType;
    private String resolvedTargetType;  // Resolved target database type (NULL if same-db, no transformation)
    private Long sourceColumnMetadataId;
    private List<ColumnTransformationAssignmentResponse> columnTransformations;
}
