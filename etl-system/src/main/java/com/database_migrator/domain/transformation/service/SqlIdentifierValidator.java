package com.database_migrator.domain.transformation.service;

import com.database_migrator.domain.connector.model.DatabaseTypeEnum;
import com.database_migrator.domain.common.exception.ValidationException;
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
            ClassPathResource resource = new ClassPathResource("migration/reserved-keywords.json");
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
            throw new ValidationException("Identifier cannot be null or empty", List.of("Invalid identifier"));
        }

        String trimmedIdentifier = identifier.trim();

        if (trimmedIdentifier.length() > MAX_IDENTIFIER_LENGTH) {
            throw new ValidationException("Identifier '" + identifier + "' exceeds maximum length of " + MAX_IDENTIFIER_LENGTH + " characters",
                    List.of("Identifier too long: " + trimmedIdentifier.length() + " characters"));
        }

        if (!SQL_IDENTIFIER_PATTERN.matcher(trimmedIdentifier).matches()) {
            throw new ValidationException("Identifier '" + identifier + "' is invalid. Must start with letter or underscore and contain only letters, numbers, and underscores",
                    List.of("Invalid identifier pattern: " + identifier));
        }

        if (isReservedKeyword(trimmedIdentifier, databaseType)) {
            throw new ValidationException("Identifier '" + identifier + "' is a reserved keyword in " + databaseType,
                    List.of("Reserved keyword: " + identifier));
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
