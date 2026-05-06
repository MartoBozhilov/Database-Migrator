package com.database_migrator.domain.connector.dto;

import com.database_migrator.domain.connector.model.ConnectorTypeEnum;
import com.database_migrator.domain.connector.model.DatabaseTypeEnum;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConnectorUpdateRequest {

    private String name;

    private DatabaseTypeEnum databaseType;

    private ConnectorTypeEnum connectorType;

    private String host;

    @Min(value = 1, message = "Port must be greater than 0")
    @Max(value = 65535, message = "Port must be less than 65536")
    private Integer port;

    private String databaseName;

    private String username;

    private String password;
}
