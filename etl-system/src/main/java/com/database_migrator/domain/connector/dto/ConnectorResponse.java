package com.database_migrator.domain.connector.dto;

import com.database_migrator.domain.connector.model.ConnectorTypeEnum;
import com.database_migrator.domain.connector.model.DatabaseTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConnectorResponse {

    private Long id;
    private String name;
    private DatabaseTypeEnum databaseType;
    private ConnectorTypeEnum connectorType;
    private String host;
    private Integer port;
    private String databaseName;
    private String username;
    private Long createdById;
    private String createdByName;
    private Date createdAt;
    private Date updatedAt;
}
