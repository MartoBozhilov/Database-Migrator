package com.database_migrator.config.database;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Identifier quote configuration (quotes, backticks, brackets)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IdentifierQuote {

    private String start; // "\"" or "`" or "["
    private String end;   // "\"" or "`" or "]"
}
