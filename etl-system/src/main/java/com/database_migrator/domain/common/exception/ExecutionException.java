package com.database_migrator.domain.common.exception;

public class ExecutionException extends DatabaseMigratorException {

    public ExecutionException(String message, Throwable cause) {
        super(message, "EXECUTION_FAILED", cause);
    }

    public ExecutionException(String message) {
        super(message, "EXECUTION_FAILED");
    }

    public ExecutionException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, cause);
    }

    @Override
    public int getHttpStatus() {
        return 500;
    }
}
