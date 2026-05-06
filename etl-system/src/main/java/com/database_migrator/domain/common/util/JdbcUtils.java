package com.database_migrator.domain.common.util;

import com.database_migrator.domain.connector.model.Connector;
import com.database_migrator.domain.connector.model.DatabaseTypeEnum;

public final class JdbcUtils {

    private JdbcUtils() {
        // prevent instantiation
    }

    public static String buildJdbcUrl(String host, Integer port, String database, DatabaseTypeEnum dbType) {
        return String.format("jdbc:%s://%s:%d/%s",
                dbType.name().toLowerCase(),
                host,
                port,
                database);
    }

    public static String buildJdbcUrl(Connector connector) {
        return buildJdbcUrl(
                connector.getHost(),
                connector.getPort(),
                connector.getDatabaseName(),
                connector.getDatabaseType()
        );
    }
}
