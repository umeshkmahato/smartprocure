package com.smartprocure.common.constants;

/**
 * Application-wide security constants shared across all modules.
 *
 * <p>Centralising these values prevents magic strings scattered through controllers,
 * filters, and service layers.
 */
public final class SecurityConstants {

    private SecurityConstants() {
        throw new UnsupportedOperationException("Constants class");
    }

    // ── HTTP Headers ────────────────────────────────────────────────────────
    public static final String AUTHORIZATION_HEADER  = "Authorization";
    public static final String BEARER_PREFIX         = "Bearer ";
    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    // ── Roles ───────────────────────────────────────────────────────────────
    public static final String ROLE_EMPLOYEE         = "EMPLOYEE";
    public static final String ROLE_MANAGER          = "MANAGER";
    public static final String ROLE_PROCUREMENT_ADMIN = "PROCUREMENT_ADMIN";
    public static final String ROLE_FINANCE          = "FINANCE";
    public static final String ROLE_ADMIN            = "ADMIN";

    // ── API ─────────────────────────────────────────────────────────────────
    public static final String API_V1 = "/api/v1";

    // ── Public endpoints (no auth required) ────────────────────────────────
    public static final String[] PUBLIC_ENDPOINTS = {
            API_V1 + "/auth/login",
            API_V1 + "/auth/register",
            API_V1 + "/auth/refresh",
            "/actuator/health",
            "/actuator/info",
            "/actuator/prometheus",
            "/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**"
    };
}
