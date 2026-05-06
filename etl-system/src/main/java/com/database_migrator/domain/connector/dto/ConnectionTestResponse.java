package com.database_migrator.domain.connector.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionTestResponse {

    private boolean success;
    private String message;
    private String databaseVersion;
}
