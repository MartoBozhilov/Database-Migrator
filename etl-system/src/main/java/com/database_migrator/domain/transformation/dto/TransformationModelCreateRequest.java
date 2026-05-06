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
public class TransformationModelCreateRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotNull(message = "System scan ID is required")
    private Long systemScanId;

    @NotNull(message = "Target connector ID is required")
    private Long targetConnectorId;
}
