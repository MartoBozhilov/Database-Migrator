package com.database_migrator.config.migration.loaders;

import com.database_migrator.config.migration.models.MetadataQueryConfig;
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
public class MetadataQueryLoader {

    private final Map<DatabaseTypeEnum, MetadataQueryConfig> queryConfigs = new HashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void loadQueries() {
        loadQueryConfig(DatabaseTypeEnum.MYSQL, "migration/metadata-queries/mysql.json");
        loadQueryConfig(DatabaseTypeEnum.POSTGRESQL, "migration/metadata-queries/postgresql.json");
        loadQueryConfig(DatabaseTypeEnum.MSSQL, "migration/metadata-queries/mssql.json");
        log.info("Loaded metadata query configurations for {} database types", queryConfigs.size());
    }

    private void loadQueryConfig(DatabaseTypeEnum dbType, String resourcePath) {
        try {
            ClassPathResource resource = new ClassPathResource(resourcePath);
            MetadataQueryConfig config = objectMapper.readValue(
                    resource.getInputStream(),
                    MetadataQueryConfig.class
            );
            queryConfigs.put(dbType, config);
            log.debug("Loaded query config for {}", dbType);
        } catch (IOException e) {
            log.error("Failed to load query config for {} from {}", dbType, resourcePath, e);
            throw new ExecutionException("Failed to load metadata query configuration", e);
        }
    }

    public MetadataQueryConfig getQueryConfig(DatabaseTypeEnum dbType) {
        MetadataQueryConfig config = queryConfigs.get(dbType);
        if (config == null) {
            throw new ValidationException("No query configuration found for database type: " + dbType,
                    List.of("Missing metadata query configuration for: " + dbType));
        }
        return config;
    }
}
