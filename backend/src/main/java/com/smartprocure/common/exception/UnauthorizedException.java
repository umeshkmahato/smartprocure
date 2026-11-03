package com.smartprocure.common.exception;

/**
 * Thrown when an action requires authentication or a specific role that
 * the current user does not possess.
 *
 * Maps to HTTP 401 or 403 depending on the {@link ErrorCode} supplied.
 */
public class UnauthorizedException extends BusinessException {

    public UnauthorizedException() {
        super(ErrorCode.UNAUTHORIZED);
    }

    public UnauthorizedException(ErrorCode errorCode) {
        super(errorCode);
    }

    public UnauthorizedException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
