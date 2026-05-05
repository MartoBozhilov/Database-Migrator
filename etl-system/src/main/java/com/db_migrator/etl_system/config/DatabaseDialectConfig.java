package com.db_migrator.etl_system.config;

import com.db_migrator.etl_system.model.enums.DatabaseTypeEnum;
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
