package com.database_migrator.domain.connector.dto;

import com.database_migrator.domain.connector.model.ConnectorTypeEnum;
import com.database_migrator.domain.connector.model.DatabaseTypeEnum;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
public class ConnectorCreateRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotNull(message = "Database type is required")
    private DatabaseTypeEnum databaseType;

    @NotNull(message = "Connector type is required")
    private ConnectorTypeEnum connectorType;

    @NotBlank(message = "Host is required")
    private String host;

    @NotNull(message = "Port is required")
    @Min(value = 1, message = "Port must be greater than 0")
    @Max(value = 65535, message = "Port must be less than 65536")
    private Integer port;

    private String databaseName;

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;
}
