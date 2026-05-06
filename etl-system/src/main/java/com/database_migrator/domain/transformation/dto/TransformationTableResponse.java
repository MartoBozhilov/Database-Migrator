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
public class TransformationTableResponse {
    private Long id;
    private String sourceTableName;
    private Long sourceTableMetadataId;
    private List<TableTransformationAssignmentResponse> tableTransformations;
    private List<TransformationColumnResponse> columns;
}
