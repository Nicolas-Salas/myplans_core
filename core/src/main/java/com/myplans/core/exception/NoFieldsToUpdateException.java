package com.myplans.core.exception;

public class NoFieldsToUpdateException extends RuntimeException {
    public NoFieldsToUpdateException(String message) {
        super(message);
    }
}