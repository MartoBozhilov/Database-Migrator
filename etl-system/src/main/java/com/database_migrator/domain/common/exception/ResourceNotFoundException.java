package com.database_migrator.domain.common.exception;

public class ResourceNotFoundException extends DatabaseMigratorException {

    public ResourceNotFoundException(String resourceType, Object id) {
        super(String.format("%s not found with id: %s", resourceType, id), "RESOURCE_NOT_FOUND");
    }

    public ResourceNotFoundException(String message) {
        super(message, "RESOURCE_NOT_FOUND");
    }

    @Override
    public int getHttpStatus() {
        return 404;
    }
}
