package com.smartprocure.purchaserequest;

import com.smartprocure.purchaserequest.dto.*;
import com.smartprocure.purchaserequest.entity.PurchaseRequestPriority;
import com.smartprocure.user.dto.LoginRequest;
import com.smartprocure.user.dto.RegisterRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("PurchaseRequest endpoints — integration")
class PurchaseRequestControllerIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("pgvector/pgvector:pg16")
                    .withDatabaseName("smartprocure_pr_it")
                    .withUsername("sp_it")
                    .withPassword("sp_it_pass");

    @Autowired private TestRestTemplate rest;

    private static String adminToken;
    private static String requesterToken;
    private static Long   createdPrId;
    private static Long   addedItemId;

    private static final String PR_URL        = "/api/v1/purchase-requests";
    private static final String AUTH_REGISTER = "/api/v1/auth/register";
    private static final String AUTH_LOGIN    = "/api/v1/auth/login";

    // ── Setup ─────────────────────────────────────────────────────────────────

    @BeforeEach
    void ensureTokens() {
        if (adminToken != null && requesterToken != null) return;
        adminToken     = registerAndLogin("pr_admin",     "pr_admin@sp.test",     "Admin@Pass1!");
        requesterToken = registerAndLogin("pr_requester", "pr_requester@sp.test", "Buyer@Pass1!");
    }

    private String registerAndLogin(String username, String email, String password) {
        var reg = new RegisterRequest(username, email, password, "Test", "User", "IT", null);
        rest.postForEntity(AUTH_REGISTER, reg, Map.class);
        ResponseEntity<Map> res = rest.postForEntity(AUTH_LOGIN, new LoginRequest(username, password), Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) res.getBody().get("data");
        return (String) data.get("accessToken");
    }

    private HttpHeaders bearer(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    private LineItemRequest sampleItem() {
        return new LineItemRequest("Laptop", "Dell XPS", BigDecimal.ONE,
                "unit", new BigDecimal("1200.00"), null, null);
    }

    // ── 1. Create ─────────────────────────────────────────────────────────────

    @Test @Order(1)
    @DisplayName("POST /purchase-requests: valid → 201 DRAFT PR created")
    void create_valid_returns201() {
        var req = new CreatePurchaseRequestRequest(
                "IT Equipment Q1", "Laptops for new hires",
                PurchaseRequestPriority.HIGH, "IT", null, "USD", null,
                List.of(sampleItem()));

        ResponseEntity<Map> res = rest.exchange(
                PR_URL, HttpMethod.POST,
                new HttpEntity<>(req, bearer(requesterToken)), Map.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) res.getBody().get("data");
        assertThat(data.get("status")).isEqualTo("DRAFT");
        assertThat(data.get("title")).isEqualTo("IT Equipment Q1");
        assertThat(data.get("prNumber").toString()).startsWith("PR-");
        createdPrId = Long.valueOf(data.get("id").toString());
    }

    @Test @Order(2)
    @DisplayName("POST /purchase-requests: empty items → 400 VALIDATION_ERROR")
    void create_emptyItems_returns400() {
        HttpEntity<String> entity = new HttpEntity<>(
                "{\"title\":\"No Items\",\"items\":[]}", bearer(requesterToken));
        ResponseEntity<Map> res = rest.exchange(PR_URL, HttpMethod.POST, entity, Map.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test @Order(3)
    @DisplayName("POST /purchase-requests: unauthenticated → 401")
    void create_unauthenticated_returns401() {
        var req = new CreatePurchaseRequestRequest("X", null, null, null, null, null, null,
                List.of(sampleItem()));
        ResponseEntity<Map> res = rest.postForEntity(PR_URL, req, Map.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ── 2. Get ────────────────────────────────────────────────────────────────

    @Test @Order(4)
    @DisplayName("GET /purchase-requests/{id}: existing → 200 with DTO")
    void getById_existing_returns200() {
        assertThat(createdPrId).isNotNull();
        ResponseEntity<Map> res = rest.exchange(
                PR_URL + "/" + createdPrId, HttpMethod.GET,
                new HttpEntity<>(bearer(requesterToken)), Map.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) res.getBody().get("data");
        assertThat(data.get("id").toString()).isEqualTo(createdPrId.toString());
    }

    @Test @Order(5)
    @DisplayName("GET /purchase-requests/{id}: non-existent → 404")
    void getById_nonExistent_returns404() {
        ResponseEntity<Map> res = rest.exchange(
                PR_URL + "/999999", HttpMethod.GET,
                new HttpEntity<>(bearer(requesterToken)), Map.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── 3. My requests ────────────────────────────────────────────────────────

    @Test @Order(6)
    @DisplayName("GET /purchase-requests/my → 200 paginated list for requester")
    void myRequests_returns200() {
        ResponseEntity<Map> res = rest.exchange(
                PR_URL + "/my?page=0&size=10", HttpMethod.GET,
                new HttpEntity<>(bearer(requesterToken)), Map.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) res.getBody().get("data");
        assertThat(data).containsKey("content");
    }

    // ── 4. Update ─────────────────────────────────────────────────────────────

    @Test @Order(7)
    @DisplayName("PUT /purchase-requests/{id}: DRAFT + owner → 200 updated")
    void update_draft_returns200() {
        assertThat(createdPrId).isNotNull();
        var req = new UpdatePurchaseRequestRequest(
                "IT Equipment Q1 Updated", null, PurchaseRequestPriority.URGENT,
                "Engineering", null, null, "Revised budget", null);

        ResponseEntity<Map> res = rest.exchange(
                PR_URL + "/" + createdPrId, HttpMethod.PUT,
                new HttpEntity<>(req, bearer(requesterToken)), Map.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) res.getBody().get("data");
        assertThat(data.get("title")).isEqualTo("IT Equipment Q1 Updated");
        assertThat(data.get("priority")).isEqualTo("URGENT");
    }

    // ── 5. Add / remove line item ─────────────────────────────────────────────

    @Test @Order(8)
    @DisplayName("POST /purchase-requests/{id}/items: DRAFT → 201 item added")
    void addItem_draft_returns201() {
        assertThat(createdPrId).isNotNull();
        var req = new LineItemRequest("Monitor", "4K Display",
                new BigDecimal("2"), "unit", new BigDecimal("500.00"), null, null);

        ResponseEntity<Map> res = rest.exchange(
                PR_URL + "/" + createdPrId + "/items", HttpMethod.POST,
                new HttpEntity<>(req, bearer(requesterToken)), Map.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) res.getBody().get("data");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) data.get("items");
        assertThat(items).hasSizeGreaterThan(1);
        addedItemId = Long.valueOf(items.stream()
                .filter(i -> "Monitor".equals(i.get("itemName")))
                .findFirst().orElseThrow()
                .get("id").toString());
    }

    @Test @Order(9)
    @DisplayName("DELETE /purchase-requests/{id}/items/{itemId}: DRAFT → 200 item removed")
    void removeItem_draft_returns200() {
        assertThat(addedItemId).isNotNull();
        ResponseEntity<Map> res = rest.exchange(
                PR_URL + "/" + createdPrId + "/items/" + addedItemId,
                HttpMethod.DELETE,
                new HttpEntity<>(bearer(requesterToken)), Map.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // ── 6. Submit ─────────────────────────────────────────────────────────────

    @Test @Order(10)
    @DisplayName("PATCH /purchase-requests/{id}/submit: DRAFT → 200 SUBMITTED")
    void submit_draft_returns200() {
        assertThat(createdPrId).isNotNull();
        ResponseEntity<Map> res = rest.exchange(
                PR_URL + "/" + createdPrId + "/submit", HttpMethod.PATCH,
                new HttpEntity<>(bearer(requesterToken)), Map.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) res.getBody().get("data");
        assertThat(data.get("status")).isEqualTo("SUBMITTED");
    }

    @Test @Order(11)
    @DisplayName("PATCH /purchase-requests/{id}/submit: SUBMITTED PR → 400 invalid transition")
    void submit_alreadySubmitted_returns400() {
        ResponseEntity<Map> res = rest.exchange(
                PR_URL + "/" + createdPrId + "/submit", HttpMethod.PATCH,
                new HttpEntity<>(bearer(requesterToken)), Map.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test @Order(12)
    @DisplayName("PUT /purchase-requests/{id}: SUBMITTED PR → 400 not editable")
    void update_submitted_returns400() {
        var req = new UpdatePurchaseRequestRequest("New title", null, null, null, null, null, null, null);
        ResponseEntity<Map> res = rest.exchange(
                PR_URL + "/" + createdPrId, HttpMethod.PUT,
                new HttpEntity<>(req, bearer(requesterToken)), Map.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ── 7. Approve ────────────────────────────────────────────────────────────

    @Test @Order(13)
    @DisplayName("PATCH /purchase-requests/{id}/approve: SUBMITTED → 200 APPROVED (admin)")
    void approve_submitted_returns200() {
        assertThat(createdPrId).isNotNull();
        ResponseEntity<Map> res = rest.exchange(
                PR_URL + "/" + createdPrId + "/approve", HttpMethod.PATCH,
                new HttpEntity<>(bearer(adminToken)), Map.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) res.getBody().get("data");
        assertThat(data.get("status")).isEqualTo("APPROVED");
    }

    @Test @Order(14)
    @DisplayName("PATCH /purchase-requests/{id}/approve: requester (non-admin) → 403")
    void approve_nonAdmin_returns403() {
        ResponseEntity<Map> res = rest.exchange(
                PR_URL + "/" + createdPrId + "/approve", HttpMethod.PATCH,
                new HttpEntity<>(bearer(requesterToken)), Map.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ── 8. Reject flow (new PR) ───────────────────────────────────────────────

    @Test @Order(15)
    @DisplayName("PATCH /purchase-requests/{id}/reject: SUBMITTED → 200 REJECTED with reason")
    void reject_submitted_returns200() {
        // Create + submit a fresh PR for reject test
        var req = new CreatePurchaseRequestRequest(
                "To Be Rejected", null, null, null, null, "USD", null,
                List.of(sampleItem()));
        ResponseEntity<Map> createRes = rest.exchange(
                PR_URL, HttpMethod.POST,
                new HttpEntity<>(req, bearer(requesterToken)), Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> created = (Map<String, Object>) createRes.getBody().get("data");
        Long rejectPrId = Long.valueOf(created.get("id").toString());

        rest.exchange(PR_URL + "/" + rejectPrId + "/submit", HttpMethod.PATCH,
                new HttpEntity<>(bearer(requesterToken)), Map.class);

        var rejectReq = new RejectPurchaseRequestRequest("Budget not available");
        ResponseEntity<Map> res = rest.exchange(
                PR_URL + "/" + rejectPrId + "/reject", HttpMethod.PATCH,
                new HttpEntity<>(rejectReq, bearer(adminToken)), Map.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) res.getBody().get("data");
        assertThat(data.get("status")).isEqualTo("REJECTED");
        assertThat(data.get("rejectionReason")).isEqualTo("Budget not available");
    }

    // ── 9. Cancel ─────────────────────────────────────────────────────────────

    @Test @Order(16)
    @DisplayName("PATCH /purchase-requests/{id}/cancel: DRAFT → 200 CANCELLED")
    void cancel_draft_returns200() {
        var req = new CreatePurchaseRequestRequest(
                "To Cancel", null, null, null, null, "USD", null,
                List.of(sampleItem()));
        ResponseEntity<Map> createRes = rest.exchange(
                PR_URL, HttpMethod.POST,
                new HttpEntity<>(req, bearer(requesterToken)), Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> created = (Map<String, Object>) createRes.getBody().get("data");
        Long cancelPrId = Long.valueOf(created.get("id").toString());

        ResponseEntity<Map> res = rest.exchange(
                PR_URL + "/" + cancelPrId + "/cancel", HttpMethod.PATCH,
                new HttpEntity<>(bearer(requesterToken)), Map.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) res.getBody().get("data");
        assertThat(data.get("status")).isEqualTo("CANCELLED");
    }

    // ── 10. Search (admin) ────────────────────────────────────────────────────

    @Test @Order(17)
    @DisplayName("GET /purchase-requests: admin search → 200 paginated results")
    void search_admin_returns200() {
        ResponseEntity<Map> res = rest.exchange(
                PR_URL + "?page=0&size=10", HttpMethod.GET,
                new HttpEntity<>(bearer(adminToken)), Map.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) res.getBody().get("data");
        assertThat(data).containsKey("content");
        assertThat(data).containsKey("totalElements");
    }

    @Test @Order(18)
    @DisplayName("GET /purchase-requests: requester (non-admin) → 403")
    void search_nonAdmin_returns403() {
        ResponseEntity<Map> res = rest.exchange(
                PR_URL + "?page=0&size=10", HttpMethod.GET,
                new HttpEntity<>(bearer(requesterToken)), Map.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
