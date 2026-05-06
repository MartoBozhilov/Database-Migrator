package com.database_migrator.domain.common.exception;

public class BusinessRuleException extends DatabaseMigratorException {

    public BusinessRuleException(String message, String errorCode) {
        super(message, errorCode);
    }

    public BusinessRuleException(String message) {
        super(message, "BUSINESS_RULE_VIOLATION");
    }

    @Override
    public int getHttpStatus() {
        return 422;
    }
}
