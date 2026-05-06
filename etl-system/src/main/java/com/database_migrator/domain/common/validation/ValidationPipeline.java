package com.database_migrator.domain.common.validation;

import com.database_migrator.domain.transformation.dto.ValidationResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Pipeline for chaining multiple validators together.
 * Executes all validators and merges their results.
 *
 * @param <T> Type to validate
 */
@Component
public class ValidationPipeline<T> {

    private final List<Validator<T>> validators = new ArrayList<>();

    /**
     * Add a validator to the pipeline.
     *
     * @param validator Validator to add
     * @return this pipeline for fluent chaining
     */
    public ValidationPipeline<T> addValidator(Validator<T> validator) {
        validators.add(validator);
        return this;
    }

    /**
     * Execute all validators and merge results.
     *
     * @param target Object to validate
     * @return Merged validation result
     */
    public ValidationResult validate(T target) {
        ValidationResult result = new ValidationResult();

        for (Validator<T> validator : validators) {
            ValidationResult validatorResult = validator.validate(target);
            result.merge(validatorResult);
        }

        return result;
    }

    /**
     * Clear all validators from the pipeline.
     */
    public void clear() {
        validators.clear();
    }

    /**
     * Get number of validators in the pipeline.
     *
     * @return Number of validators
     */
    public int size() {
        return validators.size();
    }
}
