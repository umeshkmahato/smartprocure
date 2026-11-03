package com.smartprocure.common.exception;

/**
 * Base runtime exception for all SmartProcure business rule violations.
 *
 * <p>Every domain-specific exception extends this class, carrying an
 * {@link ErrorCode} that determines the HTTP response status and
 * machine-readable error identifier returned to the client.
 *
 * <p>Caught centrally by {@link GlobalExceptionHandler}.
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
