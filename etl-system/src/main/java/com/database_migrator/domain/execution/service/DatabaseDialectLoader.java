package com.database_migrator.domain.execution.service;

import com.database_migrator.config.database.DatabaseDialectConfig;
import com.database_migrator.config.database.PaginationConfig;
import com.database_migrator.domain.connector.model.DatabaseTypeEnum;
import tools.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads database dialect configurations from JSON files
 *
 * Provides database-specific SQL syntax handling:
 * - Identifier escaping
 * - AUTO_INCREMENT definitions
 * - DEFAULT function mapping
 * - Pagination clause generation
 *
 * Extensible: add new databases by creating JSON file in resources/database-dialects/
 */
@Service
@Slf4j
public class DatabaseDialectLoader {

    private static final String DIALECT_PATH = "database-dialects/";
    private final Map<DatabaseTypeEnum, DatabaseDialectConfig> dialects = new HashMap<>();

    @PostConstruct
    public void loadDialects() {
        try {
            loadDialect("postgresql.json");
            loadDialect("mysql.json");
            loadDialect("mssql.json");

            log.info("Loaded {} database dialects", dialects.size());
        } catch (IOException e) {
            throw new RuntimeException("Failed to load database dialects", e);
        }
    }

    private void loadDialect(String fileName) throws IOException {
        ClassPathResource resource = new ClassPathResource(DIALECT_PATH + fileName);
        ObjectMapper mapper = new ObjectMapper();
        DatabaseDialectConfig config = mapper.readValue(resource.getInputStream(), DatabaseDialectConfig.class);
        dialects.put(config.getDatabaseType(), config);
        log.info("Loaded dialect: {}", config.getDatabaseType());
    }

    public DatabaseDialectConfig getDialect(DatabaseTypeEnum databaseType) {
        DatabaseDialectConfig dialect = dialects.get(databaseType);
        if (dialect == null) {
            throw new RuntimeException("Unsupported database type: " + databaseType);
        }
        return dialect;
    }

    /**
     * Escape identifier with database-specific quotes
     *
     * @param identifier Table or column name
     * @param databaseType Database type
     * @return Escaped identifier (e.g., "name", `name`, [name])
     */
    public String escapeIdentifier(String identifier, DatabaseTypeEnum databaseType) {
        DatabaseDialectConfig dialect = getDialect(databaseType);
        return dialect.getIdentifierQuote().getStart() +
               identifier +
               dialect.getIdentifierQuote().getEnd();
    }

    /**
     * Get AUTO_INCREMENT definition from dialect configuration
     *
     * @param dataType Column data type
     * @param databaseType Database type
     * @return Database-specific auto-increment syntax (e.g., SERIAL, INT AUTO_INCREMENT, INT IDENTITY(1,1))
     */
    public String getAutoIncrementDefinition(String dataType, DatabaseTypeEnum databaseType) {
        DatabaseDialectConfig dialect = getDialect(databaseType);

        // Determine key based on data type
        String key = dataType.toLowerCase().contains("bigint") ? "bigint" : "integer";
        String template = dialect.getAutoIncrement().get(key);

        if (template == null) {
            return dataType; // No auto-increment for this type
        }

        // Replace {dataType} placeholder
        return template.replace("{dataType}", dataType);
    }

    /**
     * Map default function to database-specific equivalent
     *
     * @param functionName Standard function name (e.g., CURRENT_TIMESTAMP, UUID)
     * @param databaseType Database type
     * @return Database-specific function (e.g., GETDATE(), NEWID())
     */
    public String mapDefaultFunction(String functionName, DatabaseTypeEnum databaseType) {
        DatabaseDialectConfig dialect = getDialect(databaseType);
        String upperFunction = functionName.toUpperCase();

        return dialect.getDefaultFunctions().getOrDefault(upperFunction, functionName);
    }

    /**
     * Build pagination clause from dialect template
     *
     * @param databaseType Database type
     * @param offset Row offset
     * @param limit Row limit
     * @param orderByColumn Column to order by (required for MSSQL)
     * @return Pagination clause (e.g., "LIMIT 1000 OFFSET 5000")
     */
    public String buildPaginationClause(DatabaseTypeEnum databaseType, int offset, int limit, String orderByColumn) {
        DatabaseDialectConfig dialect = getDialect(databaseType);
        PaginationConfig pagination = dialect.getPagination();

        String clause = pagination.getTemplate()
            .replace("{limit}", String.valueOf(limit))
            .replace("{offset}", String.valueOf(offset))
            .replace("{orderBy}", escapeIdentifier(orderByColumn, databaseType));

        return clause;
    }

    /**
     * Check if database requires ORDER BY for pagination
     *
     * @param databaseType Database type
     * @return true for MSSQL (requires ORDER BY), false for PostgreSQL/MySQL
     */
    public boolean requiresOrderByForPagination(DatabaseTypeEnum databaseType) {
        return getDialect(databaseType).getPagination().isRequiresOrderBy();
    }
}
