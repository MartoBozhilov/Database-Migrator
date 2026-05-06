package com.database_migrator.config.database;

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
    private Map<String, String> autoIncrement;    // "integer" -> "SERIAL" or "{dataType} AUTO_INCREMENT"
    private PaginationConfig pagination;
    private Map<String, String> defaultFunctions; // "CURRENT_TIMESTAMP" -> "GETDATE()"
}
