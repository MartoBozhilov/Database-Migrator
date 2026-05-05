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
public class RelationAddRequest {
    @NotBlank(message = "Foreign table name is required")
    private String foreignTable;

    @NotBlank(message = "Foreign column name is required")
    private String foreignColumn;

    @NotBlank(message = "Primary table name is required")
    private String primaryTable;

    @NotBlank(message = "Primary column name is required")
    private String primaryColumn;
}
