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
public class ColumnRenameRequest {
    @NotBlank(message = "New column name is required")
    private String newName;
}
