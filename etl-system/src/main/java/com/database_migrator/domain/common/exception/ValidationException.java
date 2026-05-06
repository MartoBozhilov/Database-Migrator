package com.database_migrator.domain.common.exception;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class ValidationException extends DatabaseMigratorException {

    private final List<String> errors;

    public ValidationException(String message, List<String> errors) {
        super(message, "VALIDATION_FAILED");
        this.errors = errors != null ? new ArrayList<>(errors) : new ArrayList<>();
    }

    public ValidationException(String message) {
        super(message, "VALIDATION_FAILED");
        this.errors = new ArrayList<>();
        this.errors.add(message);
    }

    @Override
    public int getHttpStatus() {
        return 400;
    }
}
