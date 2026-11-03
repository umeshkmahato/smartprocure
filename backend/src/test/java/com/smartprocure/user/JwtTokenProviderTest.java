package com.smartprocure.user;

import com.smartprocure.common.security.JwtTokenProvider;
import com.smartprocure.common.security.UserPrincipal;
import com.smartprocure.user.entity.Role;
import com.smartprocure.user.entity.RoleName;
import com.smartprocure.user.entity.User;
import com.smartprocure.user.entity.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link JwtTokenProvider}.
 * No Spring context needed — @Value fields injected via {@link ReflectionTestUtils}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtTokenProvider")
class JwtTokenProviderTest {

    /** Valid Base64-encoded secret (≥256 bits) — matches application.yml default. */
    private static final String TEST_SECRET =
            "bXktc3VwZXItc2VjcmV0LWtleS1mb3Itc21hcnRwcm9jdXJlLWFwcC0yMDI1LW11c3QtYmUtYXQtbGVhc3QtMjU2LWJpdHM=";

    private JwtTokenProvider provider;
    private UserPrincipal    principal;

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider();
        ReflectionTestUtils.setField(provider, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(provider, "accessTokenExpirySeconds", 900L);

        Role role = new Role();
        role.setId(1L);
        role.setName(RoleName.EMPLOYEE);

        User user = new User();
        user.setId(42L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPasswordHash("hash");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setStatus(UserStatus.ACTIVE);
        user.setRoles(new HashSet<>(Set.of(role)));

        principal = UserPrincipal.create(user);
    }

    @Test
    @DisplayName("generateAccessToken returns a non-null, non-blank JWT string")
    void generateAccessToken_returnsValidJwt() {
        String token = provider.generateAccessToken(principal);
        assertThat(token).isNotNull().isNotBlank().contains(".");
    }

    @Test
    @DisplayName("validateToken returns true for a freshly generated token")
    void validateToken_freshToken_returnsTrue() {
        String token = provider.generateAccessToken(principal);
        assertThat(provider.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("validateToken returns false for a tampered token")
    void validateToken_tamperedToken_returnsFalse() {
        String token = provider.generateAccessToken(principal) + "tampered";
        assertThat(provider.validateToken(token)).isFalse();
    }

    @Test
    @DisplayName("validateToken returns false for a completely invalid string")
    void validateToken_garbage_returnsFalse() {
        assertThat(provider.validateToken("not.a.valid.jwt")).isFalse();
    }

    @Test
    @DisplayName("validateToken returns false for blank input")
    void validateToken_blank_returnsFalse() {
        assertThat(provider.validateToken("")).isFalse();
        assertThat(provider.validateToken(null)).isFalse();
    }

    @Test
    @DisplayName("extractUsername returns the subject set during token generation")
    void extractUsername_returnsCorrectSubject() {
        String token = provider.generateAccessToken(principal);
        assertThat(provider.extractUsername(token)).isEqualTo("testuser");
    }

    @Test
    @DisplayName("extractUserId returns the userId claim")
    void extractUserId_returnsCorrectId() {
        String token = provider.generateAccessToken(principal);
        assertThat(provider.extractUserId(token)).isEqualTo(42L);
    }

    @Test
    @DisplayName("generateRefreshToken returns a unique non-empty string each call")
    void generateRefreshToken_uniquePerCall() {
        String rt1 = provider.generateRefreshToken();
        String rt2 = provider.generateRefreshToken();
        assertThat(rt1).isNotBlank().hasSize(64);
        assertThat(rt2).isNotBlank().hasSize(64);
        assertThat(rt1).isNotEqualTo(rt2);
    }

    @Test
    @DisplayName("getAccessTokenExpirySeconds returns the configured value")
    void getAccessTokenExpirySeconds_returnsInjectedValue() {
        assertThat(provider.getAccessTokenExpirySeconds()).isEqualTo(900L);
    }
}
