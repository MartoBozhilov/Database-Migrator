package com.database_migrator.domain.transformation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Validation result wrapper for pre-execution validation
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResult {

    private boolean valid;
    private List<String> errors;
    private List<String> warnings;

    public ValidationResult(boolean valid) {
        this.valid = valid;
        this.errors = new ArrayList<>();
        this.warnings = new ArrayList<>();
    }

    public void addError(String error) {
        this.errors.add(error);
        this.valid = false;
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public String getErrorMessages() {
        return String.join("; ", errors);
    }

    public static ValidationResult success() {
        return new ValidationResult(true);
    }

    public static ValidationResult error(String message) {
        ValidationResult result = new ValidationResult(false);
        result.addError(message);
        return result;
    }
}
