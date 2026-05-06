package com.database_migrator.domain.transformation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ColumnAddRequest {
    @NotBlank(message = "Column name is required")
    private String columnName;

    @NotBlank(message = "Data type is required")
    private String dataType;

    @NotNull(message = "isNullable flag is required")
    private Boolean isNullable;

    @NotNull(message = "isPrimaryKey flag is required")
    private Boolean isPrimaryKey;

    private String defaultValue;
}
