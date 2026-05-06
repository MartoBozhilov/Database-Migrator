package com.database_migrator.domain.transformation.dto;

import com.database_migrator.domain.transformation.model.TableTransformationType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TableTransformationAssignmentResponse {
    private Long id;
    private TableTransformationType transformationType;

    // For RENAME_TABLE
    private String newName;

    // For ADD_TABLE
    private String tableName;
    private String idGenerationStrategy;
}
