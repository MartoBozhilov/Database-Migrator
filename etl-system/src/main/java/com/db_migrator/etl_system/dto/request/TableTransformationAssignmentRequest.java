package com.db_migrator.etl_system.dto.request;

import com.db_migrator.etl_system.model.enums.TableTransformationType;
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
