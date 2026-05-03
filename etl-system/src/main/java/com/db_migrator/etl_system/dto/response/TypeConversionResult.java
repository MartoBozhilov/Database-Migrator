package com.db_migrator.etl_system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TypeConversionResult {
    private boolean valid;
    private boolean hasWarning;
    private String warning;
    private String errorMessage;

    public static TypeConversionResult success() {
        return new TypeConversionResult(true, false, null, null);
    }

    public static TypeConversionResult successWithWarning(String warning) {
        return new TypeConversionResult(true, true, warning, null);
    }

    public static TypeConversionResult failure(String errorMessage) {
        return new TypeConversionResult(false, false, null, errorMessage);
    }
}
