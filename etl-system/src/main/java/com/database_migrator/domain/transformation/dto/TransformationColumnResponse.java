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
    private String resolvedTargetType;
    private Long sourceColumnMetadataId;
    private Boolean isPrimaryKey;
    private Boolean isForeignKey;
    private List<ColumnTransformationAssignmentResponse> columnTransformations;
}
