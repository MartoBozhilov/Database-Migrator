package com.database_migrator.domain.common.validation;

import com.database_migrator.domain.transformation.dto.ValidationResult;

/**
 * Generic validator interface for business validation.
 * Implementations validate specific entities or requests.
 *
 * @param <T> Type to validate
 */
public interface Validator<T> {

    /**
     * Validate the given target object.
     *
     * @param target Object to validate
     * @return ValidationResult with errors and warnings
     */
    ValidationResult validate(T target);
}
