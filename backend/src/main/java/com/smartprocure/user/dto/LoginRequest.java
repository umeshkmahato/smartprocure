package com.smartprocure.user.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Accepts either username or email as the identifier.
 * Intentionally does NOT distinguish between "user not found" and
 * "wrong password" in error messages — avoids user enumeration.
 */
public record LoginRequest(
        @NotBlank(message = "Username or email is required")
        String usernameOrEmail,

        @NotBlank(message = "Password is required")
        String password
) {}
