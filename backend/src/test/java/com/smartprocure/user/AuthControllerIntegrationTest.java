package com.smartprocure.user;

import com.smartprocure.user.dto.LoginRequest;
import com.smartprocure.user.dto.RegisterRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end integration test for the authentication REST endpoints.
 *
 * <p>Requires Docker (Testcontainers pulls {@code pgvector/pgvector:pg16}).
 * Run with: {@code mvn clean verify} (Failsafe lifecycle).
 *
 * <p>Tests run in a defined order so that the registered user is available
 * for the login and /me tests.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Auth endpoints — integration")
class AuthControllerIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("pgvector/pgvector:pg16")
                    .withDatabaseName("smartprocure_it")
                    .withUsername("sp_it")
                    .withPassword("sp_it_pass");

    @Autowired
    private TestRestTemplate rest;

    private static final String REGISTER_URL = "/api/v1/auth/register";
    private static final String LOGIN_URL     = "/api/v1/auth/login";
    private static final String ME_URL        = "/api/v1/auth/me";

    /** Shared between tests to verify the full login → /me flow. */
    private static String sharedAccessToken;

    // ── Register ──────────────────────────────────────────────────────────────

    @Test @Order(1)
    @DisplayName("POST /register: valid payload → 201 with user DTO")
    void register_validRequest_returns201() {
        var req = new RegisterRequest(
                "ituser", "it@example.com", "Integration@1!",
                "Integration", "User", "IT", null);

        ResponseEntity<Map> response = rest.postForEntity(REGISTER_URL, req, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        assertThat(data).isNotNull();
        assertThat(data.get("username")).isEqualTo("ituser");
        assertThat(data.get("email")).isEqualTo("it@example.com");
        assertThat(data.get("roles")).isNotNull();
    }

    @Test @Order(2)
    @DisplayName("POST /register: duplicate email → 409 CONFLICT")
    void register_duplicateEmail_returns409() {
        var req = new RegisterRequest(
                "ituser2", "it@example.com", "Integration@1!",
                "Dup", "User", null, null);

        ResponseEntity<Map> response = rest.postForEntity(REGISTER_URL, req, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test @Order(3)
    @DisplayName("POST /register: missing required field → 400 VALIDATION_ERROR")
    void register_missingField_returns400() {
        // Send an empty body
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>("{}", headers);

        ResponseEntity<Map> response = rest.postForEntity(REGISTER_URL, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsKey("errorCode");
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    @Test @Order(4)
    @DisplayName("POST /login: valid credentials → 200 with access + refresh tokens")
    void login_validCredentials_returns200WithTokens() {
        var req = new LoginRequest("ituser", "Integration@1!");

        ResponseEntity<Map> response = rest.postForEntity(LOGIN_URL, req, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        assertThat(data).isNotNull();
        assertThat(data.get("accessToken")).isNotNull().asString().isNotBlank();
        assertThat(data.get("refreshToken")).isNotNull().asString().isNotBlank();
        assertThat(data.get("tokenType")).isEqualTo("Bearer");

        sharedAccessToken = (String) data.get("accessToken");
    }

    @Test @Order(5)
    @DisplayName("POST /login: wrong password → 401 INVALID_CREDENTIALS")
    void login_wrongPassword_returns401() {
        var req = new LoginRequest("ituser", "WrongPassword@1!");

        ResponseEntity<Map> response = rest.postForEntity(LOGIN_URL, req, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test @Order(6)
    @DisplayName("POST /login: unknown user → 401 INVALID_CREDENTIALS (no enumeration)")
    void login_unknownUser_returns401() {
        var req = new LoginRequest("nobody@example.com", "Password@1!");

        ResponseEntity<Map> response = rest.postForEntity(LOGIN_URL, req, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ── /me ───────────────────────────────────────────────────────────────────

    @Test @Order(7)
    @DisplayName("GET /me: with valid JWT → 200 with authenticated user")
    void me_withValidJwt_returns200() {
        assertThat(sharedAccessToken).as("Login must succeed before /me test").isNotNull();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(sharedAccessToken);

        ResponseEntity<Map> response = rest.exchange(
                ME_URL, HttpMethod.GET, new HttpEntity<>(headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        assertThat(data.get("username")).isEqualTo("ituser");
    }

    @Test @Order(8)
    @DisplayName("GET /me: without JWT → 401 UNAUTHORIZED")
    void me_withoutJwt_returns401() {
        ResponseEntity<Map> response = rest.getForEntity(ME_URL, Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
