package com.smartprocure.user.controller;

import com.smartprocure.common.response.ApiResponse;
import com.smartprocure.common.response.PageResponse;
import com.smartprocure.user.dto.ChangePasswordRequest;
import com.smartprocure.user.dto.UpdateProfileRequest;
import com.smartprocure.user.dto.UserDto;
import com.smartprocure.user.entity.UserStatus;
import com.smartprocure.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

/**
 * User-management REST endpoints.
 *
 * <p>Authorisation rules:
 * <ul>
 *   <li>Listing all users — {@code ADMIN} only.</li>
 *   <li>Reading / updating a user — {@code ADMIN} or the user themselves.</li>
 *   <li>Changing password — the user themselves only.</li>
 *   <li>Status and role changes — {@code ADMIN} only.</li>
 * </ul>
 *
 * <p>{@code @userService.isCurrentUser(#id)} calls
 * {@link UserService#isCurrentUser(Long)} via Spring Security SpEL;
 * it reads the currently authenticated user from the SecurityContext.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "User CRUD, role assignment and status management")
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all users — ADMIN only (paginated)")
    public ResponseEntity<ApiResponse<PageResponse<UserDto>>> list(
            @RequestParam(defaultValue = "0")              int    page,
            @RequestParam(defaultValue = "20")             int    size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        return ResponseEntity.ok(ApiResponse.success(userService.findAll(page, size, sort)));
    }

    @GetMapping("/me")
    @Operation(summary = "Return the currently authenticated user's profile")
    public ResponseEntity<ApiResponse<UserDto>> me() {
        return ResponseEntity.ok(ApiResponse.success(userService.getCurrentUser()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @userService.isCurrentUser(#id)")
    @Operation(summary = "Get user by ID — ADMIN or own account")
    public ResponseEntity<ApiResponse<UserDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(userService.findById(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @userService.isCurrentUser(#id)")
    @Operation(summary = "Update profile (firstName, lastName, department, phone)")
    public ResponseEntity<ApiResponse<UserDto>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.success(userService.updateProfile(id, request)));
    }

    @PutMapping("/{id}/password")
    @PreAuthorize("@userService.isCurrentUser(#id)")
    @Operation(summary = "Change own password (own account only)")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @PathVariable Long id,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(id, request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully"));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Activate, deactivate or lock a user — ADMIN only")
    public ResponseEntity<ApiResponse<UserDto>> updateStatus(
            @PathVariable Long id,
            @RequestParam UserStatus status) {
        return ResponseEntity.ok(ApiResponse.success(userService.updateStatus(id, status)));
    }

    @PutMapping("/{id}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Replace the user's role set — ADMIN only")
    public ResponseEntity<ApiResponse<UserDto>> updateRoles(
            @PathVariable Long id,
            @RequestBody Set<String> roleNames) {
        return ResponseEntity.ok(ApiResponse.success(userService.updateRoles(id, roleNames)));
    }
}
