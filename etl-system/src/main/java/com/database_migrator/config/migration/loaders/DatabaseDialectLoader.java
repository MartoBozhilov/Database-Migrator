package com.database_migrator.config.migration.loaders;

import com.database_migrator.config.migration.models.DatabaseDialectConfig;
import com.database_migrator.config.migration.models.PaginationConfig;
import com.database_migrator.domain.connector.model.DatabaseTypeEnum;
import com.database_migrator.domain.common.exception.ExecutionException;
import com.database_migrator.domain.common.exception.ValidationException;
import tools.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads database dialect configurations from JSON files.
 * Provides database-specific SQL syntax handling:
 * - Identifier escaping
 * - AUTO_INCREMENT definitions
 * - DEFAULT function mapping
 * - Pagination clause generation
 */
@Service
@Slf4j
public class DatabaseDialectLoader {

    private static final String DIALECT_PATH = "migration/database-dialects/";
    private final Map<DatabaseTypeEnum, DatabaseDialectConfig> dialects = new HashMap<>();

    @PostConstruct
    public void loadDialects() {
        try {
            loadDialect("postgresql.json");
            loadDialect("mysql.json");
            loadDialect("mssql.json");

            log.info("Loaded {} database dialects", dialects.size());
        } catch (IOException e) {
            throw new ExecutionException("Failed to load database dialects", e);
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
            throw new ValidationException("Unsupported database type: " + databaseType, List.of("Database type not supported: " + databaseType));
        }
        return dialect;
    }

    /**
     * Escape identifier with database-specific quotes
     */
    public String escapeIdentifier(String identifier, DatabaseTypeEnum databaseType) {
        DatabaseDialectConfig dialect = getDialect(databaseType);
        return dialect.getIdentifierQuote().getStart() +
                identifier +
                dialect.getIdentifierQuote().getEnd();
    }

    /**
     * Get AUTO_INCREMENT definition from dialect configuration
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
     */
    public String mapDefaultFunction(String functionName, DatabaseTypeEnum databaseType) {
        DatabaseDialectConfig dialect = getDialect(databaseType);
        String upperFunction = functionName.toUpperCase();

        return dialect.getDefaultFunctions().getOrDefault(upperFunction, functionName);
    }

    /**
     * Build pagination clause from dialect template
     */
    public String buildPaginationClause(DatabaseTypeEnum databaseType, int offset, int limit, String orderByColumn) {
        DatabaseDialectConfig dialect = getDialect(databaseType);
        PaginationConfig pagination = dialect.getPagination();

        return pagination.getTemplate()
                .replace("{limit}", String.valueOf(limit))
                .replace("{offset}", String.valueOf(offset))
                .replace("{orderBy}", escapeIdentifier(orderByColumn, databaseType));
    }

    /**
     * Check if database requires ORDER BY for pagination
     */
    public boolean requiresOrderByForPagination(DatabaseTypeEnum databaseType) {
        return getDialect(databaseType).getPagination().isRequiresOrderBy();
    }
}
