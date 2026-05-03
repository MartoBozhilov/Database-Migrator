package com.db_migrator.etl_system.dto.request;

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
public class TransformationTableRequest {

    @NotNull(message = "Source table metadata ID is required")
    private Long sourceTableMetadataId;

    private List<TableTransformationAssignmentRequest> tableTransformations;
    private List<TransformationColumnRequest> columns;
}
