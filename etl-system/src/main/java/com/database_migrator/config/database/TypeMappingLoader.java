package com.database_migrator.config.database;

import com.database_migrator.domain.connector.model.DatabaseTypeEnum;
import com.database_migrator.domain.common.exception.ExecutionException;
import com.database_migrator.domain.common.exception.ValidationException;
import tools.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class TypeMappingLoader {

    private final Map<String, TypeMappingConfig> typeMappings = new HashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void loadMappings() {
        loadTypeMapping(DatabaseTypeEnum.MYSQL, DatabaseTypeEnum.POSTGRESQL, "type-mappings/mysql-to-postgresql.json");
        loadTypeMapping(DatabaseTypeEnum.MYSQL, DatabaseTypeEnum.MSSQL, "type-mappings/mysql-to-mssql.json");
        loadTypeMapping(DatabaseTypeEnum.POSTGRESQL, DatabaseTypeEnum.MYSQL, "type-mappings/postgresql-to-mysql.json");
        loadTypeMapping(DatabaseTypeEnum.POSTGRESQL, DatabaseTypeEnum.MSSQL, "type-mappings/postgresql-to-mssql.json");
        loadTypeMapping(DatabaseTypeEnum.MSSQL, DatabaseTypeEnum.MYSQL, "type-mappings/mssql-to-mysql.json");
        loadTypeMapping(DatabaseTypeEnum.MSSQL, DatabaseTypeEnum.POSTGRESQL, "type-mappings/mssql-to-postgresql.json");
        log.info("Loaded type mappings for {} database pairs", typeMappings.size());
    }

    private void loadTypeMapping(DatabaseTypeEnum sourceDb, DatabaseTypeEnum targetDb, String resourcePath) {
        try {
            ClassPathResource resource = new ClassPathResource(resourcePath);
            TypeMappingConfig config = objectMapper.readValue(
                    resource.getInputStream(),
                    TypeMappingConfig.class
            );
            String key = buildKey(sourceDb, targetDb);
            typeMappings.put(key, config);
            log.debug("Loaded type mapping for {} -> {}", sourceDb, targetDb);
        } catch (IOException e) {
            log.error("Failed to load type mapping for {} -> {} from {}", sourceDb, targetDb, resourcePath, e);
            throw new ExecutionException("Failed to load type mapping configuration", e);
        }
    }

    public TypeMappingConfig getMapping(DatabaseTypeEnum sourceDb, DatabaseTypeEnum targetDb) {
        String key = buildKey(sourceDb, targetDb);
        TypeMappingConfig config = typeMappings.get(key);
        if (config == null) {
            throw new ValidationException("No type mapping found for " + sourceDb + " -> " + targetDb,
                    List.of("Missing type mapping configuration"));
        }
        return config;
    }

    public boolean isValidTypeConversion(String sourceType, DatabaseTypeEnum sourceDb,
                                         String targetType, DatabaseTypeEnum targetDb) {
        TypeMappingConfig config = getMapping(sourceDb, targetDb);
        List<TypeMapping> allowedMappings = config.getMappings().get(sourceType.toUpperCase());

        if (allowedMappings == null || allowedMappings.isEmpty()) {
            return false;
        }

        return allowedMappings.stream()
                .anyMatch(mapping -> mapping.getTargetType().equalsIgnoreCase(targetType));
    }

    public List<TypeMapping> getAllowedTargetTypes(String sourceType, DatabaseTypeEnum sourceDb,
                                                   DatabaseTypeEnum targetDb) {
        // Same database - return source type as mapping
        if (sourceDb == targetDb) {
            TypeMapping sameType = new TypeMapping();
            sameType.setTargetType(sourceType);
            sameType.setDataLossRisk(false);
            return List.of(sameType);
        }

        TypeMappingConfig config = getMapping(sourceDb, targetDb);
        String normalizedSourceType = normalizeType(sourceType);

        return config.getMappings().getOrDefault(normalizedSourceType, List.of());
    }

    public boolean isValidTargetType(String targetType, DatabaseTypeEnum targetDb) {
        String normalizedTargetType = normalizeType(targetType);

        // Check all mappings where this database is the target
        for (DatabaseTypeEnum sourceDb : DatabaseTypeEnum.values()) {
            if (sourceDb == targetDb) {
                // Same database - check if type exists as a source type in any mapping
                try {
                    TypeMappingConfig config = getMapping(targetDb, DatabaseTypeEnum.MYSQL); // Any other db
                    if (config.getMappings().containsKey(normalizedTargetType)) {
                        return true;
                    }
                } catch (Exception e) {
                    // Ignore, try next
                }
                continue;
            }

            try {
                TypeMappingConfig config = getMapping(sourceDb, targetDb);

                // Check if this type appears as a target type in any mapping
                for (List<TypeMapping> mappings : config.getMappings().values()) {
                    for (TypeMapping mapping : mappings) {
                        if (normalizeType(mapping.getTargetType()).equals(normalizedTargetType)) {
                            return true;
                        }
                    }
                }
            } catch (Exception e) {
                // Mapping doesn't exist, continue
            }
        }

        return false;
    }

    private String normalizeType(String type) {
        if (type == null) {
            return null;
        }
        // Extract base type (e.g., "VARCHAR(100)" -> "VARCHAR")
        String baseType = type.trim().toUpperCase();
        int parenIndex = baseType.indexOf('(');
        if (parenIndex > 0) {
            baseType = baseType.substring(0, parenIndex);
        }
        return baseType;
    }

    private String buildKey(DatabaseTypeEnum sourceDb, DatabaseTypeEnum targetDb) {
        return sourceDb.name() + "_TO_" + targetDb.name();
    }
}
