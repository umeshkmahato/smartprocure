package com.smartprocure.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.smartprocure.common.exception.ErrorCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Standard error response envelope for all SmartProcure error payloads.
 *
 * <pre>
 * {
 *   "correlationId": "uuid",
 *   "errorCode":     "RESOURCE_NOT_FOUND",
 *   "status":        404,
 *   "message":       "User not found with id: '42'",
 *   "errors":        [{"field":"email","message":"must not be blank","rejectedValue":""}],
 *   "timestamp":     "2025-01-01T12:00:00"
 * }
 * </pre>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String correlationId,
        String errorCode,
        int status,
        String message,
        List<FieldErrorDetail> errors,
        LocalDateTime timestamp
) {

    /** Per-field validation error detail. */
    public record FieldErrorDetail(
            String field,
            String message,
            Object rejectedValue
    ) {}

    // ── Factory methods ────────────────────────────────────────────────────

    public static ErrorResponse of(ErrorCode code, String message, String correlationId) {
        return new ErrorResponse(
                correlationId,
                code.name(),
                code.getHttpStatus().value(),
                (message != null && !message.isBlank()) ? message : code.getDefaultMessage(),
                null,
                LocalDateTime.now()
        );
    }

    public static ErrorResponse of(ErrorCode code, String message, String correlationId,
                                   List<FieldErrorDetail> errors) {
        return new ErrorResponse(
                correlationId,
                code.name(),
                code.getHttpStatus().value(),
                (message != null && !message.isBlank()) ? message : code.getDefaultMessage(),
                errors,
                LocalDateTime.now()
        );
    }
}
