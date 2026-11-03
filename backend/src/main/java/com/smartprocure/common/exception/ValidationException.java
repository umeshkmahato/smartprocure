package com.smartprocure.common.exception;

/**
 * Thrown when a business-layer validation rule is violated.
 * Maps to HTTP 400 Bad Request via {@link GlobalExceptionHandler}.
 */
public class ValidationException extends BusinessException {

    public ValidationException(String message) {
        super(ErrorCode.VALIDATION_ERROR, message);
    }

    public ValidationException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
