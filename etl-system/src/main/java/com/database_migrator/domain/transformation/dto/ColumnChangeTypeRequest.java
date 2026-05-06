package com.database_migrator.domain.transformation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ColumnChangeTypeRequest {
    @NotBlank(message = "Target data type is required")
    private String targetDataType;
}
