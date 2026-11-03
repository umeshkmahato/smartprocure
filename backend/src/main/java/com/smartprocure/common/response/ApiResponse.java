package com.smartprocure.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.slf4j.MDC;

import java.time.LocalDateTime;

/**
 * Standard API response envelope for all SmartProcure REST endpoints.
 *
 * <p>Every controller method returns {@code ResponseEntity<ApiResponse<T>>}
 * so the frontend always receives a consistent JSON structure:
 * <pre>
 * {
 *   "success": true,
 *   "message": "...",
 *   "data": { ... },
 *   "correlationId": "uuid",
 *   "timestamp": "2025-01-01T12:00:00"
 * }
 * </pre>
 *
 * <p>The {@code correlationId} is propagated from the MDC set by
 * {@link com.smartprocure.common.filter.CorrelationIdFilter}, enabling
 * end-to-end request tracing across logs and distributed traces.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        String correlationId,
        LocalDateTime timestamp
) {

    // ── Factory methods ────────────────────────────────────────────────────

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "Success", data, correlationId(), LocalDateTime.now());
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, message, data, correlationId(), LocalDateTime.now());
    }

    /** Used when the response carries a message only (no data body). */
    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>(true, message, null, correlationId(), LocalDateTime.now());
    }

    /** Semantic alias for 201 Created responses. */
    public static <T> ApiResponse<T> created(T data) {
        return new ApiResponse<>(true, "Resource created successfully", data,
                correlationId(), LocalDateTime.now());
    }

    /** Semantic alias for 201 Created responses with a custom message. */
    public static <T> ApiResponse<T> created(T data, String message) {
        return new ApiResponse<>(true, message, data, correlationId(), LocalDateTime.now());
    }

    // ── Internal helpers ───────────────────────────────────────────────────

    private static String correlationId() {
        return MDC.get("correlationId");
    }
}
