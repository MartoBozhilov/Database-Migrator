package com.database_migrator.domain.common.util;

import com.database_migrator.domain.connector.model.Connector;
import com.database_migrator.domain.connector.model.DatabaseTypeEnum;

public final class JdbcUtils {

    private JdbcUtils() {
        // prevent instantiation
    }

    public static String buildJdbcUrl(String host, Integer port, String database, DatabaseTypeEnum dbType) {
        return switch (dbType) {
            case MSSQL ->
                    String.format("jdbc:sqlserver://%s:%d;databaseName=%s;encrypt=true;trustServerCertificate=true",
                            host, port, database);
            case MYSQL ->
                    String.format("jdbc:mysql://%s:%d/%s?useSSL=true&requireSSL=true&verifyServerCertificate=false",
                            host, port, database);
            case POSTGRESQL ->
                // TODO enable ssl on local postgres server
                    String.format("jdbc:postgresql://%s:%d/%s?sslmode=prefer",
                            host, port, database);
        };
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
