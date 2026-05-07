package com.database_migrator.config.migration.models;

import com.database_migrator.domain.connector.model.DatabaseTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DatabaseDialectConfig {

    private DatabaseTypeEnum databaseType;
    private IdentifierQuote identifierQuote;
    private Map<String, String> autoIncrement;
    private PaginationConfig pagination;
    private Map<String, String> defaultFunctions;
}
