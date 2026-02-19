package com.inventory.exception;

public class CustomFieldValidationException extends RuntimeException {

    public CustomFieldValidationException(String message) {
        super(message);
    }
}
