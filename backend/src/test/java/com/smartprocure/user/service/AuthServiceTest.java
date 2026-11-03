package com.smartprocure.user.service;

import com.smartprocure.common.audit.AuditService;
import com.smartprocure.common.exception.BusinessException;
import com.smartprocure.common.exception.ErrorCode;
import com.smartprocure.common.security.JwtTokenProvider;
import com.smartprocure.user.dto.LoginRequest;
import com.smartprocure.user.dto.LoginResponse;
import com.smartprocure.user.dto.RegisterRequest;
import com.smartprocure.user.dto.UserDto;
import com.smartprocure.user.entity.RefreshToken;
import com.smartprocure.user.entity.Role;
import com.smartprocure.user.entity.RoleName;
import com.smartprocure.user.entity.User;
import com.smartprocure.user.entity.UserStatus;
import com.smartprocure.user.repository.RefreshTokenRepository;
import com.smartprocure.user.repository.RoleRepository;
import com.smartprocure.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

    @Mock private UserRepository         userRepository;
    @Mock private RoleRepository         roleRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder        passwordEncoder;
    @Mock private JwtTokenProvider       jwtTokenProvider;
    @Mock private AuditService           auditService;

    @InjectMocks
    private AuthService authService;

    private Role employeeRole;
    private User testUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshTokenExpirySeconds", 604800L);

        employeeRole = new Role();
        employeeRole.setId(1L);
        employeeRole.setName(RoleName.EMPLOYEE);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash("encoded");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setStatus(UserStatus.ACTIVE);
        testUser.setRoles(new HashSet<>(Set.of(employeeRole)));
    }

    // ── register ─────────────────────────────────────────────────────────────

    @Test @DisplayName("register: new user → returns UserDto with correct fields")
    void register_newUser_returnsUserDto() {
        var req = new RegisterRequest(
                "newuser", "new@example.com", "Password@1!", "New", "User", "IT", null);

        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(roleRepository.findByName(RoleName.EMPLOYEE)).thenReturn(Optional.of(employeeRole));
        when(passwordEncoder.encode("Password@1!")).thenReturn("encoded");
        when(userRepository.save(any())).thenReturn(testUser);

        UserDto result = authService.register(req);

        assertThat(result).isNotNull();
        assertThat(result.username()).isEqualTo("testuser");
        verify(auditService).log(eq("USER"), any(), eq("REGISTER"), anyString());
    }

    @Test @DisplayName("register: duplicate email → EMAIL_ALREADY_EXISTS")
    void register_duplicateEmail_throws() {
        var req = new RegisterRequest(
                "user2", "test@example.com", "Password@1!", "A", "B", null, null);

        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS));
    }

    @Test @DisplayName("register: duplicate username → USER_ALREADY_EXISTS")
    void register_duplicateUsername_throws() {
        var req = new RegisterRequest(
                "testuser", "unique@example.com", "Password@1!", "A", "B", null, null);

        when(userRepository.existsByEmail("unique@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.USER_ALREADY_EXISTS));
    }

    // ── login ─────────────────────────────────────────────────────────────────

    @Test @DisplayName("login: valid credentials → LoginResponse with tokens")
    void login_validCredentials_returnsLoginResponse() {
        var req = new LoginRequest("testuser", "Password@1!");

        when(userRepository.findByUsernameOrEmail("testuser", "testuser"))
                .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("Password@1!", "encoded")).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(any())).thenReturn("access.jwt");
        when(jwtTokenProvider.generateRefreshToken()).thenReturn("refresh-uuid");
        when(jwtTokenProvider.getAccessTokenExpirySeconds()).thenReturn(900L);
        when(refreshTokenRepository.save(any())).thenReturn(new RefreshToken());

        LoginResponse result = authService.login(req);

        assertThat(result.accessToken()).isEqualTo("access.jwt");
        assertThat(result.refreshToken()).isEqualTo("refresh-uuid");
        assertThat(result.tokenType()).isEqualTo("Bearer");
        assertThat(result.expiresIn()).isEqualTo(900L);
    }

    @Test @DisplayName("login: user not found → INVALID_CREDENTIALS (no user enumeration)")
    void login_unknownUser_throwsInvalidCredentials() {
        when(userRepository.findByUsernameOrEmail(anyString(), anyString()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("nobody", "pw")))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_CREDENTIALS));
    }

    @Test @DisplayName("login: wrong password → INVALID_CREDENTIALS")
    void login_wrongPassword_throwsInvalidCredentials() {
        when(userRepository.findByUsernameOrEmail("testuser", "testuser"))
                .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("testuser", "wrong")))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_CREDENTIALS));
    }

    @Test @DisplayName("login: inactive user → ACCOUNT_DISABLED")
    void login_inactiveUser_throwsAccountDisabled() {
        testUser.setStatus(UserStatus.INACTIVE);
        when(userRepository.findByUsernameOrEmail("testuser", "testuser"))
                .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("pw", "encoded")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(new LoginRequest("testuser", "pw")))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.ACCOUNT_DISABLED));
    }

    // ── logout ────────────────────────────────────────────────────────────────

    @Test @DisplayName("logout: valid token → token revoked")
    void logout_validToken_revokesToken() {
        RefreshToken rt = RefreshToken.builder()
                .token("valid-token")
                .revoked(false)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        when(refreshTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(rt));
        when(refreshTokenRepository.save(any())).thenReturn(rt);

        authService.logout("valid-token");

        assertThat(rt.isRevoked()).isTrue();
    }

    @Test @DisplayName("logout: null token → no-op, no DB interaction")
    void logout_nullToken_isNoop() {
        assertThatCode(() -> authService.logout(null)).doesNotThrowAnyException();
        verifyNoInteractions(refreshTokenRepository);
    }

    @Test @DisplayName("logout: blank token → no-op")
    void logout_blankToken_isNoop() {
        assertThatCode(() -> authService.logout("   ")).doesNotThrowAnyException();
        verifyNoInteractions(refreshTokenRepository);
    }
}
