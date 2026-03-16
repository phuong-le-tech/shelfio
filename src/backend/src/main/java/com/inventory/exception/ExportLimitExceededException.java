package com.inventory.exception;

public class ExportLimitExceededException extends RuntimeException {
    public ExportLimitExceededException(String message) {
        super(message);
    }
}
