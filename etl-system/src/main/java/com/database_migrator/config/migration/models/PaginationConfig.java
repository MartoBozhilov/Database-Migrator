package com.database_migrator.config.migration.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaginationConfig {

    private String type;              // "LIMIT_OFFSET" or "OFFSET_FETCH"
    private String template;          // "LIMIT {limit} OFFSET {offset}"
    private boolean requiresOrderBy;  // true for MSSQL
}
