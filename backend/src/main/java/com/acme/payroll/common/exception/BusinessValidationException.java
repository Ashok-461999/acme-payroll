package com.acme.payroll.common.exception;

public class BusinessValidationException extends RuntimeException {

    private final String field;

    public BusinessValidationException(String message) {
        super(message);
        this.field = null;
    }

    public BusinessValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
