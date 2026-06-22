package com.database_migrator.domain.execution.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles MySQL ENUM type extraction and PostgreSQL ENUM type generation
 */
@Component
@Slf4j
public class EnumTypeHandler {

    private static final Pattern ENUM_PATTERN = Pattern.compile("^ENUM\\((.+)\\)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern SET_PATTERN = Pattern.compile("^SET\\((.+)\\)$", Pattern.CASE_INSENSITIVE);

    public boolean isEnumType(String dataType) {
        if (dataType == null) {
            return false;
        }
        return dataType.toUpperCase().startsWith("ENUM(");
    }

    public boolean isSetType(String dataType) {
        if (dataType == null) {
            return false;
        }
        return dataType.toUpperCase().startsWith("SET(");
    }

    public List<String> extractEnumValues(String enumDefinition) {
        List<String> values = new ArrayList<>();

        if (enumDefinition == null) {
            return values;
        }

        Matcher matcher = ENUM_PATTERN.matcher(enumDefinition.trim());
        if (matcher.matches()) {
            String valuesPart = matcher.group(1);
            // Split by comma, but handle quoted values properly
            String[] parts = valuesPart.split(",");
            for (String part : parts) {
                String cleaned = part.trim()
                        .replaceAll("^'", "")  // Remove leading quote
                        .replaceAll("'$", ""); // Remove trailing quote
                values.add(cleaned);
            }
        }

        log.debug("Extracted ENUM values from {}: {}", enumDefinition, values);
        return values;
    }

    public String generateEnumTypeName(String tableName, String columnName) {
        return String.format("%s_%s_enum", tableName, columnName)
                .toLowerCase()
                .replaceAll("[^a-z0-9_]", "_");
    }

    public String generateCreateTypeStatement(String typeName, List<String> values) {
        if (values.isEmpty()) {
            log.warn("Cannot create ENUM type {} with empty values", typeName);
            return null;
        }

        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TYPE ").append(typeName).append(" AS ENUM (");

        for (int i = 0; i < values.size(); i++) {
            sql.append("'").append(values.get(i)).append("'");
            if (i < values.size() - 1) {
                sql.append(", ");
            }
        }

        sql.append(")");

        log.debug("Generated CREATE TYPE statement: {}", sql);
        return sql.toString();
    }

    public String generateDropTypeStatement(String typeName) {
        return String.format("DROP TYPE IF EXISTS %s CASCADE", typeName);
    }
}
