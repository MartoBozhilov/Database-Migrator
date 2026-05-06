package com.database_migrator.domain.transformation.dto;

import com.database_migrator.domain.execution.dto.ValidationWarning;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class TransformationModelDetailsResponse extends TransformationModelResponse {
    private List<TransformationTableResponse> tables;
    private List<TransformationRelationResponse> relations;
    private List<ValidationWarning> warnings;
}
