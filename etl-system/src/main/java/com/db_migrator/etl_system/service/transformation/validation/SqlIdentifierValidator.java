package com.db_migrator.etl_system.service.transformation.validation;

import com.db_migrator.etl_system.model.enums.DatabaseTypeEnum;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Service for validating SQL identifiers (table names, column names)
 * - Loads reserved keywords from JSON file
 * - Validates against database-specific reserved keywords
 * - Validates SQL identifier rules (alphanumeric, underscores, no spaces)
 */
@Service
@Slf4j
public class SqlIdentifierValidator {

    private Map<String, List<String>> reservedKeywordsByDatabase;
    private static final Pattern SQL_IDENTIFIER_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");
    private static final int MAX_IDENTIFIER_LENGTH = 64;

    @PostConstruct
    public void loadReservedKeywords() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ClassPathResource resource = new ClassPathResource("reserved-keywords.json");
            reservedKeywordsByDatabase = mapper.readValue(
                    resource.getInputStream(),
                    new TypeReference<>() {
                    }
            );

            log.info("Loaded reserved keywords for {} databases", reservedKeywordsByDatabase.size());
        } catch (IOException e) {
            log.error("Failed to load reserved keywords from JSON", e);
            // Fallback to empty map to avoid NPE
            reservedKeywordsByDatabase = new HashMap<>();
        }
    }

    public void validateIdentifier(String identifier, DatabaseTypeEnum databaseType) {
        if (identifier == null || identifier.trim().isEmpty()) {
            throw new RuntimeException("Identifier cannot be null or empty");
        }

        String trimmedIdentifier = identifier.trim();

        // Check length
        if (trimmedIdentifier.length() > MAX_IDENTIFIER_LENGTH) {
            throw new RuntimeException("Identifier '" + identifier + "' exceeds maximum length of " + MAX_IDENTIFIER_LENGTH + " characters");
        }

        // Check SQL identifier pattern (alphanumeric + underscores, must start with letter or underscore)
        if (!SQL_IDENTIFIER_PATTERN.matcher(trimmedIdentifier).matches()) {
            throw new RuntimeException("Identifier '" + identifier + "' is invalid. Must start with letter or underscore and contain only letters, numbers, and underscores");
        }

        // Check reserved keywords
        if (isReservedKeyword(trimmedIdentifier, databaseType)) {
            throw new RuntimeException("Identifier '" + identifier + "' is a reserved keyword in " + databaseType);
        }
    }

    public boolean isReservedKeyword(String identifier, DatabaseTypeEnum databaseType) {
        if (identifier == null || databaseType == null) {
            return false;
        }

        String upperIdentifier = identifier.toUpperCase();
        List<String> keywords = reservedKeywordsByDatabase.get(databaseType.name());

        if (keywords == null) {
            log.warn("No reserved keywords found for database type: {}", databaseType);
            return false;
        }

        return keywords.contains(upperIdentifier);
    }
}
