package com.db_migrator.etl_system.dto.response;

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
