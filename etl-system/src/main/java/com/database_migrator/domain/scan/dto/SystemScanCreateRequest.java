package com.database_migrator.domain.scan.dto;

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
public class SystemScanCreateRequest {

    @NotBlank(message = "Scan name is required")
    private String name;

    @NotNull(message = "Source connector ID is required")
    private Long sourceConnectorId;
}
