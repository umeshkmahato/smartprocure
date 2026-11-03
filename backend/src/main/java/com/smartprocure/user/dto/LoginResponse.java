package com.smartprocure.user.dto;

/**
 * Returned on successful login or token refresh.
 *
 * <p>Phase 9 (frontend) stores {@code accessToken} in memory (never localStorage)
 * and {@code refreshToken} in an HttpOnly cookie set by the server.
 */
public record LoginResponse(
        String  accessToken,
        String  refreshToken,
        String  tokenType,
        long    expiresIn,   // seconds until access token expires
        UserDto user
) {}
