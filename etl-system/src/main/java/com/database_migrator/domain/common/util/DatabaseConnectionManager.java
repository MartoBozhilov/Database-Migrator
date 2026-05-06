package com.database_migrator.domain.common.util;

import com.database_migrator.domain.common.exception.ExecutionException;
import com.database_migrator.domain.connector.model.Connector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Centralized manager for database connections.
 * Handles connection creation, configuration, and cleanup.
 */
@Component
@Slf4j
public class DatabaseConnectionManager {

    public Connection createConnection(Connector connector) {
        String jdbcUrl = JdbcUtils.buildJdbcUrl(connector);

        try {
            log.debug("Creating connection to {} at {}",
                    connector.getDatabaseType(),
                    connector.getHost());

            Connection conn = DriverManager.getConnection(
                    jdbcUrl,
                    connector.getUsername(),
                    connector.getPassword()
            );

            // Disable auto-commit for batch operations
            conn.setAutoCommit(false);

            log.debug("Connection established successfully");
            return conn;

        } catch (SQLException e) {
            log.error("Failed to create connection to {}: {}", jdbcUrl, e.getMessage());
            throw new ExecutionException(
                    String.format("Failed to connect to database at %s: %s",
                            connector.getHost(), e.getMessage()),
                    e
            );
        }
    }

    public ConnectionPair createConnectionPair(Connector source, Connector target) {
        Connection sourceConn = null;
        Connection targetConn = null;

        try {
            sourceConn = createConnection(source);
            targetConn = createConnection(target);
            return new ConnectionPair(sourceConn, targetConn);

        } catch (Exception e) {
            // Clean up any opened connections if pair creation fails
            closeQuietly(sourceConn);
            closeQuietly(targetConn);
            throw e;
        }
    }

    public void closeQuietly(Connection... connections) {
        for (Connection conn : connections) {
            if (conn != null) {
                try {
                    if (!conn.isClosed()) {
                        conn.close();
                    }
                } catch (SQLException e) {
                    log.warn("Failed to close connection: {}", e.getMessage());
                }
            }
        }
    }
}
