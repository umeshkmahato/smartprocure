package com.smartprocure.user.controller;

import com.smartprocure.common.response.ApiResponse;
import com.smartprocure.user.dto.LoginRequest;
import com.smartprocure.user.dto.LoginResponse;
import com.smartprocure.user.dto.RefreshTokenRequest;
import com.smartprocure.user.dto.RegisterRequest;
import com.smartprocure.user.dto.UserDto;
import com.smartprocure.user.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication endpoints — all public (no JWT required).
 *
 * <p>Design decisions:
 * <ul>
 *   <li>Login errors return {@code 401} with generic message to prevent
 *       user-enumeration attacks.</li>
 *   <li>Refresh tokens returned in the response body; Phase 9 moves them
 *       into HttpOnly cookies on the React side.</li>
 *   <li>Logout accepts the refresh token body and revokes it server-side;
 *       the client must discard the access token locally.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register, login, refresh token and logout")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user account (default role: EMPLOYEE)")
    public ResponseEntity<ApiResponse<UserDto>> register(
            @Valid @RequestBody RegisterRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(authService.register(request), "Registration successful"));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate and receive an access + refresh token pair")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(authService.login(request), "Login successful"));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Exchange a valid refresh token for a new access token (rotation)")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest body) {
        return ResponseEntity.ok(
                ApiResponse.success(authService.refreshToken(body.refreshToken()), "Token refreshed"));
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke the supplied refresh token")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestBody(required = false) RefreshTokenRequest body) {
        authService.logout(body != null ? body.refreshToken() : null);
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
    }

    @GetMapping("/me")
    @Operation(summary = "Return the currently authenticated user's profile")
    public ResponseEntity<ApiResponse<UserDto>> me() {
        return ResponseEntity.ok(ApiResponse.success(authService.getCurrentUser()));
    }
}
