package com.db_migrator.etl_system.dto.request;

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
