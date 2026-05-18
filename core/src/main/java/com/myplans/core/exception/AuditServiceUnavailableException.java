package com.myplans.core.exception;

public class AuditServiceUnavailableException extends RuntimeException {
    public AuditServiceUnavailableException(String message) {
        super(message);
    }

    public AuditServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}