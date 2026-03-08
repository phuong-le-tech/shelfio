package com.inventory.exception;

public class ListLimitExceededException extends RuntimeException {
    public ListLimitExceededException(String message) {
        super(message);
    }
}
