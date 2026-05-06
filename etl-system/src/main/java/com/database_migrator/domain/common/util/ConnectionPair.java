package com.database_migrator.domain.common.util;

import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Holds a pair of database connections (source and target) for data migration.
 * Implements AutoCloseable to ensure connections are properly closed.
 */
@Slf4j
public record ConnectionPair(Connection source, Connection target) implements AutoCloseable {

    @Override
    public void close() {
        closeConnection(source, "source");
        closeConnection(target, "target");
    }

    private void closeConnection(Connection conn, String name) {
        if (conn != null) {
            try {
                if (!conn.isClosed()) {
                    conn.close();
                    log.debug("Closed {} connection", name);
                }
            } catch (SQLException e) {
                log.warn("Failed to close {} connection: {}", name, e.getMessage());
            }
        }
    }
}
