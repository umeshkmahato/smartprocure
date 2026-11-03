package com.smartprocure.user.service;

import com.smartprocure.common.audit.AuditService;
import com.smartprocure.common.exception.BusinessException;
import com.smartprocure.common.exception.ErrorCode;
import com.smartprocure.common.exception.ResourceNotFoundException;
import com.smartprocure.common.security.JwtTokenProvider;
import com.smartprocure.common.security.UserPrincipal;
import com.smartprocure.common.util.SecurityUtils;
import com.smartprocure.user.dto.LoginRequest;
import com.smartprocure.user.dto.LoginResponse;
import com.smartprocure.user.dto.RegisterRequest;
import com.smartprocure.user.dto.UserDto;
import com.smartprocure.user.entity.RefreshToken;
import com.smartprocure.user.entity.RoleName;
import com.smartprocure.user.entity.User;
import com.smartprocure.user.entity.UserStatus;
import com.smartprocure.user.repository.RefreshTokenRepository;
import com.smartprocure.user.repository.RoleRepository;
import com.smartprocure.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Authentication business logic.
 *
 * <p>Handles register / login / refresh / logout without delegating
 * to Spring Security's {@code AuthenticationManager} — the manager
 * is not needed because we verify passwords directly and issue JWTs.
 *
 * <p>Refresh-token rotation: on each {@link #refreshToken} call the old token
 * is revoked and a new one is issued, ensuring single-use refresh tokens.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthService {

    private final UserRepository         userRepository;
    private final RoleRepository         roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder        passwordEncoder;
    private final JwtTokenProvider       jwtTokenProvider;
    private final AuditService           auditService;

    @Value("${app.jwt.refresh-token-expiry-seconds:604800}")
    private long refreshTokenExpirySeconds;

    // ── Register ──────────────────────────────────────────────────────────────

    public UserDto register(RegisterRequest request) {
        String email    = request.email().toLowerCase().trim();
        String username = request.username().trim();

        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        if (userRepository.existsByUsername(username)) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
        }

        var employeeRole = roleRepository.findByName(RoleName.EMPLOYEE)
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR,
                        "EMPLOYEE role not found — check Flyway migration V7"));

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setDepartment(request.department());
        user.setPhone(request.phone());
        user.setStatus(UserStatus.ACTIVE);
        user.setRoles(new HashSet<>(Set.of(employeeRole)));

        User saved = userRepository.save(user);
        auditService.log("USER", saved.getId(), "REGISTER", "User registered: " + email);
        log.info("User registered: {}", email);
        return UserDto.fromEntity(saved);
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    public LoginResponse login(LoginRequest request) {
        String id = request.usernameOrEmail().trim();

        User user = userRepository.findByUsernameOrEmail(id, id)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        if (!user.isActive()) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }

        UserPrincipal principal   = UserPrincipal.create(user);
        String        accessToken  = jwtTokenProvider.generateAccessToken(principal);
        String        refreshToken = jwtTokenProvider.generateRefreshToken();

        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .token(refreshToken)
                .expiresAt(LocalDateTime.now().plusSeconds(refreshTokenExpirySeconds))
                .build());

        auditService.log("USER", user.getId(), "LOGIN", "User logged in");
        return new LoginResponse(accessToken, refreshToken, "Bearer",
                jwtTokenProvider.getAccessTokenExpirySeconds(), UserDto.fromEntity(user));
    }

    // ── Refresh ───────────────────────────────────────────────────────────────

    public LoginResponse refreshToken(String rawToken) {
        if (!StringUtils.hasText(rawToken)) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID, "Refresh token is required");
        }

        RefreshToken stored = refreshTokenRepository.findByTokenAndRevokedFalse(rawToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));

        if (!stored.isValid()) {
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED);
        }

        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        User          user       = stored.getUser();
        UserPrincipal principal  = UserPrincipal.create(user);
        String        newAccess  = jwtTokenProvider.generateAccessToken(principal);
        String        newRefresh = jwtTokenProvider.generateRefreshToken();

        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .token(newRefresh)
                .expiresAt(LocalDateTime.now().plusSeconds(refreshTokenExpirySeconds))
                .build());

        return new LoginResponse(newAccess, newRefresh, "Bearer",
                jwtTokenProvider.getAccessTokenExpirySeconds(), UserDto.fromEntity(user));
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    public void logout(String rawToken) {
        if (!StringUtils.hasText(rawToken)) return;
        refreshTokenRepository.findByToken(rawToken)
                .filter(rt -> !rt.isRevoked())
                .ifPresent(rt -> {
                    rt.setRevoked(true);
                    refreshTokenRepository.save(rt);
                });
    }

    // ── Current user ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public UserDto getCurrentUser() {
        String username = SecurityUtils.requireCurrentUsername();
        return UserDto.fromEntity(
                userRepository.findByUsernameOrEmail(username, username)
                              .orElseThrow(() -> new ResourceNotFoundException("User", "username", username)));
    }
}
