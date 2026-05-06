package com.database_migrator.domain.common.exception;

import lombok.Getter;

/**
 * Base exception for all custom exceptions in the Database Migrator application.
 */
@Getter
public abstract class DatabaseMigratorException extends RuntimeException {

    private final String errorCode;

    public DatabaseMigratorException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public DatabaseMigratorException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public abstract int getHttpStatus();
}
