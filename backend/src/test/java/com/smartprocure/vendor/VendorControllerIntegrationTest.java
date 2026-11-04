package com.smartprocure.vendor;

import com.smartprocure.user.dto.LoginRequest;
import com.smartprocure.user.dto.RegisterRequest;
import com.smartprocure.vendor.dto.AddContactRequest;
import com.smartprocure.vendor.dto.AddRatingRequest;
import com.smartprocure.vendor.dto.CreateVendorRequest;
import com.smartprocure.vendor.dto.UpdateVendorRequest;
import org.junit.jupiter.api.BeforeEach;
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
 * End-to-end integration tests for vendor REST endpoints.
 *
 * <p>Uses Testcontainers (PostgreSQL) so a real DB is wired.
 * Tests are ordered to build state incrementally:
 * register → login → create vendor → approve → add contact → add rating.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Vendor endpoints — integration")
class VendorControllerIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("pgvector/pgvector:pg16")
                    .withDatabaseName("smartprocure_vendor_it")
                    .withUsername("sp_it")
                    .withPassword("sp_it_pass");

    @Autowired
    private TestRestTemplate rest;

    private static String adminToken;
    private static String buyerToken;
    private static Long   createdVendorId;
    private static Long   createdContactId;

    private static final String VENDORS_URL  = "/api/v1/vendors";
    private static final String AUTH_REGISTER = "/api/v1/auth/register";
    private static final String AUTH_LOGIN    = "/api/v1/auth/login";

    // ── Setup: register admin + buyer, obtain tokens ───────────────────────

    @BeforeEach
    void ensureTokens() {
        // Idempotent — only fetches tokens once per test class run
        if (adminToken != null && buyerToken != null) return;
        adminToken  = registerAndLogin("vendor_admin",  "vadmin@sp.test",  "Admin@Pass1!");
        buyerToken  = registerAndLogin("vendor_buyer",  "vbuyer@sp.test",  "Buyer@Pass1!");
    }

    private String registerAndLogin(String username, String email, String password) {
        var regReq = new RegisterRequest(username, email, password, "Test", "User", "IT", null);
        rest.postForEntity(AUTH_REGISTER, regReq, Map.class); // idempotent — may 409 on re-run

        var loginReq = new LoginRequest(username, password);
        ResponseEntity<Map> res = rest.postForEntity(AUTH_LOGIN, loginReq, Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) res.getBody().get("data");
        return (String) data.get("accessToken");
    }

    private HttpHeaders bearerHeaders(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    // ── 1. Create vendor ──────────────────────────────────────────────────

    @Test @Order(1)
    @DisplayName("POST /vendors: valid payload → 201 PENDING vendor created")
    void createVendor_valid_returns201() {
        var req = new CreateVendorRequest(
                "Integration Supplies Ltd", "it-vendor@example.com",
                "+440000000", "1 Test Street", "TAX-IT-001",
                "An integration-test vendor", "https://it-vendor.example.com");

        ResponseEntity<Map> res = rest.exchange(
                VENDORS_URL, HttpMethod.POST,
                new HttpEntity<>(req, bearerHeaders(adminToken)), Map.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) res.getBody().get("data");
        assertThat(data.get("name")).isEqualTo("Integration Supplies Ltd");
        assertThat(data.get("status")).isEqualTo("PENDING");
        createdVendorId = Long.valueOf(data.get("id").toString());
    }

    @Test @Order(2)
    @DisplayName("POST /vendors: duplicate email → 409 CONFLICT")
    void createVendor_duplicateEmail_returns409() {
        var req = new CreateVendorRequest(
                "Duplicate Corp", "it-vendor@example.com",
                null, null, null, null, null);

        ResponseEntity<Map> res = rest.exchange(
                VENDORS_URL, HttpMethod.POST,
                new HttpEntity<>(req, bearerHeaders(adminToken)), Map.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test @Order(3)
    @DisplayName("POST /vendors: missing name → 400 VALIDATION_ERROR")
    void createVendor_missingName_returns400() {
        HttpHeaders headers = bearerHeaders(adminToken);
        HttpEntity<String> entity = new HttpEntity<>("{\"email\":\"x@y.com\"}", headers);

        ResponseEntity<Map> res = rest.exchange(
                VENDORS_URL, HttpMethod.POST, entity, Map.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test @Order(4)
    @DisplayName("POST /vendors: unauthenticated → 401 UNAUTHORIZED")
    void createVendor_unauthenticated_returns401() {
        var req = new CreateVendorRequest("No Auth", "noauth@x.com", null, null, null, null, null);
        ResponseEntity<Map> res = rest.postForEntity(VENDORS_URL, req, Map.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ── 2. Get vendor ─────────────────────────────────────────────────────

    @Test @Order(5)
    @DisplayName("GET /vendors/{id}: existing id → 200 with vendor DTO")
    void getVendor_existing_returns200() {
        assertThat(createdVendorId).isNotNull();

        ResponseEntity<Map> res = rest.exchange(
                VENDORS_URL + "/" + createdVendorId, HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(buyerToken)), Map.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) res.getBody().get("data");
        assertThat(data.get("id").toString()).isEqualTo(createdVendorId.toString());
    }

    @Test @Order(6)
    @DisplayName("GET /vendors/{id}: non-existent id → 404 NOT_FOUND")
    void getVendor_nonExistent_returns404() {
        ResponseEntity<Map> res = rest.exchange(
                VENDORS_URL + "/999999", HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(buyerToken)), Map.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── 3. Search vendors ─────────────────────────────────────────────────

    @Test @Order(7)
    @DisplayName("GET /vendors: paginated search returns results")
    void searchVendors_returns200WithPage() {
        ResponseEntity<Map> res = rest.exchange(
                VENDORS_URL + "?page=0&size=10", HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(buyerToken)), Map.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) res.getBody().get("data");
        assertThat(data).containsKey("content");
        assertThat(data).containsKey("totalElements");
    }

    // ── 4. Update vendor ──────────────────────────────────────────────────

    @Test @Order(8)
    @DisplayName("PUT /vendors/{id}: valid update → 200 with updated fields")
    void updateVendor_valid_returns200() {
        assertThat(createdVendorId).isNotNull();
        var req = new UpdateVendorRequest("Updated Name", null, "+999", null, null, "Updated desc", null);

        ResponseEntity<Map> res = rest.exchange(
                VENDORS_URL + "/" + createdVendorId, HttpMethod.PUT,
                new HttpEntity<>(req, bearerHeaders(adminToken)), Map.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) res.getBody().get("data");
        assertThat(data.get("name")).isEqualTo("Updated Name");
        assertThat(data.get("description")).isEqualTo("Updated desc");
    }

    // ── 5. Approve workflow ───────────────────────────────────────────────

    @Test @Order(9)
    @DisplayName("PATCH /vendors/{id}/approve: PENDING vendor → 200 APPROVED")
    void approveVendor_pending_returns200() {
        assertThat(createdVendorId).isNotNull();

        ResponseEntity<Map> res = rest.exchange(
                VENDORS_URL + "/" + createdVendorId + "/approve", HttpMethod.PATCH,
                new HttpEntity<>(bearerHeaders(adminToken)), Map.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) res.getBody().get("data");
        assertThat(data.get("status")).isEqualTo("APPROVED");
    }

    @Test @Order(10)
    @DisplayName("PATCH /vendors/{id}/approve: already APPROVED → 400")
    void approveVendor_alreadyApproved_returns400() {
        assertThat(createdVendorId).isNotNull();

        ResponseEntity<Map> res = rest.exchange(
                VENDORS_URL + "/" + createdVendorId + "/approve", HttpMethod.PATCH,
                new HttpEntity<>(bearerHeaders(adminToken)), Map.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ── 6. Contacts ───────────────────────────────────────────────────────

    @Test @Order(11)
    @DisplayName("POST /vendors/{id}/contacts: valid → 201 contact created")
    void addContact_valid_returns201() {
        assertThat(createdVendorId).isNotNull();
        var req = new AddContactRequest("Jane Contact", "jane@it-vendor.com", "+111", "Manager", true);

        ResponseEntity<Map> res = rest.exchange(
                VENDORS_URL + "/" + createdVendorId + "/contacts", HttpMethod.POST,
                new HttpEntity<>(req, bearerHeaders(adminToken)), Map.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) res.getBody().get("data");
        assertThat(data.get("name")).isEqualTo("Jane Contact");
        createdContactId = Long.valueOf(data.get("id").toString());
    }

    @Test @Order(12)
    @DisplayName("GET /vendors/{id}/contacts → 200 list includes added contact")
    void getContacts_returns200() {
        assertThat(createdVendorId).isNotNull();

        ResponseEntity<Map> res = rest.exchange(
                VENDORS_URL + "/" + createdVendorId + "/contacts", HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(buyerToken)), Map.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        java.util.List<?> data = (java.util.List<?>) res.getBody().get("data");
        assertThat(data).isNotEmpty();
    }

    @Test @Order(13)
    @DisplayName("DELETE /vendors/{id}/contacts/{cid} → 200 contact removed")
    void removeContact_valid_returns200() {
        assertThat(createdContactId).isNotNull();

        ResponseEntity<Map> res = rest.exchange(
                VENDORS_URL + "/" + createdVendorId + "/contacts/" + createdContactId,
                HttpMethod.DELETE,
                new HttpEntity<>(bearerHeaders(adminToken)), Map.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // ── 7. Ratings ────────────────────────────────────────────────────────

    @Test @Order(14)
    @DisplayName("POST /vendors/{id}/ratings: score 1–5 → 201 rating saved")
    void addRating_valid_returns201() {
        assertThat(createdVendorId).isNotNull();
        var req = new AddRatingRequest(5, "Excellent service");

        ResponseEntity<Map> res = rest.exchange(
                VENDORS_URL + "/" + createdVendorId + "/ratings", HttpMethod.POST,
                new HttpEntity<>(req, bearerHeaders(buyerToken)), Map.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) res.getBody().get("data");
        assertThat(data.get("score")).isEqualTo(5);
    }

    @Test @Order(15)
    @DisplayName("POST /vendors/{id}/ratings: score > 5 → 400 VALIDATION_ERROR")
    void addRating_outOfRange_returns400() {
        assertThat(createdVendorId).isNotNull();
        var req = new AddRatingRequest(6, "Invalid");

        ResponseEntity<Map> res = rest.exchange(
                VENDORS_URL + "/" + createdVendorId + "/ratings", HttpMethod.POST,
                new HttpEntity<>(req, bearerHeaders(buyerToken)), Map.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test @Order(16)
    @DisplayName("GET /vendors/{id}/ratings → 200 paginated ratings list")
    void getRatings_returns200() {
        assertThat(createdVendorId).isNotNull();

        ResponseEntity<Map> res = rest.exchange(
                VENDORS_URL + "/" + createdVendorId + "/ratings?page=0&size=10",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(buyerToken)), Map.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) res.getBody().get("data");
        assertThat(data).containsKey("content");
    }

    // ── 8. Suspend / deactivate ───────────────────────────────────────────

    @Test @Order(17)
    @DisplayName("PATCH /vendors/{id}/suspend: APPROVED vendor → 200 SUSPENDED")
    void suspendVendor_approved_returns200() {
        assertThat(createdVendorId).isNotNull();

        ResponseEntity<Map> res = rest.exchange(
                VENDORS_URL + "/" + createdVendorId + "/suspend", HttpMethod.PATCH,
                new HttpEntity<>(bearerHeaders(adminToken)), Map.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) res.getBody().get("data");
        assertThat(data.get("status")).isEqualTo("SUSPENDED");
    }

    @Test @Order(18)
    @DisplayName("DELETE /vendors/{id}: admin deactivates → 200 vendor inactive")
    void deactivateVendor_admin_returns200() {
        assertThat(createdVendorId).isNotNull();

        ResponseEntity<Map> res = rest.exchange(
                VENDORS_URL + "/" + createdVendorId, HttpMethod.DELETE,
                new HttpEntity<>(bearerHeaders(adminToken)), Map.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
