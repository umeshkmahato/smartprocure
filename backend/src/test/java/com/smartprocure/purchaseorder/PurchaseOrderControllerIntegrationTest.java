package com.smartprocure.purchaseorder;

import com.smartprocure.purchaseorder.dto.*;
import com.smartprocure.user.dto.LoginRequest;
import com.smartprocure.user.dto.RegisterRequest;
import com.smartprocure.vendor.dto.CreateVendorRequest;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("PurchaseOrder endpoints — integration")
class PurchaseOrderControllerIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("pgvector/pgvector:pg16")
                    .withDatabaseName("smartprocure_po_it")
                    .withUsername("sp_it")
                    .withPassword("sp_it_pass");

    @Autowired private TestRestTemplate rest;

    private static String adminToken;
    private static Long   vendorId;
    private static Long   createdPoId;

    private static final String PO_URL        = "/api/v1/purchase-orders";
    private static final String AUTH_REGISTER = "/api/v1/auth/register";
    private static final String AUTH_LOGIN    = "/api/v1/auth/login";
    private static final String VENDOR_URL    = "/api/v1/vendors";

    // ── Bootstrap ─────────────────────────────────────────────────────────────

    @BeforeEach
    void ensureBootstrap() {
        if (adminToken != null) return;
        adminToken = registerAndLogin("po_admin", "po_admin@sp.test", "Admin@Pass1!");
        vendorId   = createAndApproveVendor();
    }

    private String registerAndLogin(String username, String email, String password) {
        var reg = new RegisterRequest(username, email, password, "PO", "Admin", "Procurement", null);
        rest.postForEntity(AUTH_REGISTER, reg, Map.class);
        ResponseEntity<Map> res = rest.postForEntity(
                AUTH_LOGIN, new LoginRequest(username, password), Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) res.getBody().get("data");
        return (String) data.get("accessToken");
    }

    private Long createAndApproveVendor() {
        var req = new CreateVendorRequest(
                "Acme Supplies", "acme_po@vendor.test",
                "+1-555-0100", "123 Vendor St", "New York", "USA",
                "TAX-PO-001", "REG-PO-001",
                "Office Supplies", "USD", null);
        ResponseEntity<Map> res = rest.exchange(
                VENDOR_URL, HttpMethod.POST,
                new HttpEntity<>(req, bearer(adminToken)), Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) res.getBody().get("data");
        Long id = Long.valueOf(data.get("id").toString());

        rest.exchange(VENDOR_URL + "/" + id + "/approve", HttpMethod.PATCH,
                new HttpEntity<>(bearer(adminToken)), Map.class);
        return id;
    }

    private HttpHeaders bearer(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    private CreatePurchaseOrderRequest buildCreateReq() {
        return new CreatePurchaseOrderRequest(
                vendorId, null, null, "USD",
                LocalDate.now().plusDays(30),
                "123 Delivery Lane", "Net 30", "Integration test PO",
                List.of(new PoLineItemRequest(
                        "Office Chair", "Ergonomic",
                        new BigDecimal("5"), "unit",
                        new BigDecimal("250.00"), null)));
    }

    // ── 1. Create ─────────────────────────────────────────────────────────────

    @Test @Order(1)
    @DisplayName("POST /purchase-orders: valid → 201 DRAFT PO created")
    void create_valid_returns201() {
        ResponseEntity<Map> res = rest.exchange(
                PO_URL, HttpMethod.POST,
                new HttpEntity<>(buildCreateReq(), bearer(adminToken)), Map.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) res.getBody().get("data");
        assertThat(data.get("status")).isEqualTo("DRAFT");
        assertThat(data.get("poNumber").toString()).startsWith("PO-");
        assertThat(data.get("vendorId").toString()).isEqualTo(vendorId.toString());
        createdPoId = Long.valueOf(data.get("id").toString());
    }

    @Test @Order(2)
    @DisplayName("POST /purchase-orders: missing vendorId → 400")
    void create_missingVendorId_returns400() {
        HttpEntity<String> entity = new HttpEntity<>(
                "{\"items\":[{\"itemName\":\"X\",\"quantity\":1,\"unitPrice\":10}]}",
                bearer(adminToken));
        ResponseEntity<Map> res = rest.exchange(PO_URL, HttpMethod.POST, entity, Map.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test @Order(3)
    @DisplayName("POST /purchase-orders: unauthenticated → 401")
    void create_unauthenticated_returns401() {
        ResponseEntity<Map> res = rest.postForEntity(PO_URL, buildCreateReq(), Map.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ── 2. Get by id ──────────────────────────────────────────────────────────

    @Test @Order(4)
    @DisplayName("GET /purchase-orders/{id}: existing → 200")
    void getById_existing_returns200() {
        assertThat(createdPoId).isNotNull();
        ResponseEntity<Map> res = rest.exchange(
                PO_URL + "/" + createdPoId, HttpMethod.GET,
                new HttpEntity<>(bearer(adminToken)), Map.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) res.getBody().get("data");
        assertThat(data.get("id").toString()).isEqualTo(createdPoId.toString());
        assertThat(data.get("totalAmount")).isNotNull();
    }

    @Test @Order(5)
    @DisplayName("GET /purchase-orders/{id}: non-existent → 404")
    void getById_notFound_returns404() {
        ResponseEntity<Map> res = rest.exchange(
                PO_URL + "/999999", HttpMethod.GET,
                new HttpEntity<>(bearer(adminToken)), Map.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── 3. Search ─────────────────────────────────────────────────────────────

    @Test @Order(6)
    @DisplayName("GET /purchase-orders: paginated search → 200")
    void search_returns200() {
        ResponseEntity<Map> res = rest.exchange(
                PO_URL + "?page=0&size=10", HttpMethod.GET,
                new HttpEntity<>(bearer(adminToken)), Map.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) res.getBody().get("data");
        assertThat(data).containsKey("content");
        assertThat(data).containsKey("totalElements");
    }

    @Test @Order(7)
    @DisplayName("GET /purchase-orders: filter by status=DRAFT → 200 filtered results")
    void search_filterByStatus_returns200() {
        ResponseEntity<Map> res = rest.exchange(
                PO_URL + "?status=DRAFT&page=0&size=10", HttpMethod.GET,
                new HttpEntity<>(bearer(adminToken)), Map.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // ── 4. Update ─────────────────────────────────────────────────────────────

    @Test @Order(8)
    @DisplayName("PUT /purchase-orders/{id}: DRAFT → 200 updated fields")
    void update_draft_returns200() {
        assertThat(createdPoId).isNotNull();
        var req = new UpdatePurchaseOrderRequest(
                "EUR", LocalDate.now().plusDays(60),
                "456 Updated Lane", "Net 60", "Updated notes", null);

        ResponseEntity<Map> res = rest.exchange(
                PO_URL + "/" + createdPoId, HttpMethod.PUT,
                new HttpEntity<>(req, bearer(adminToken)), Map.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) res.getBody().get("data");
        assertThat(data.get("currency")).isEqualTo("EUR");
        assertThat(data.get("paymentTerms")).isEqualTo("Net 60");
    }

    // ── 5. Issue ──────────────────────────────────────────────────────────────

    @Test @Order(9)
    @DisplayName("PATCH /purchase-orders/{id}/issue: DRAFT → 200 ISSUED")
    void issue_draft_returns200() {
        assertThat(createdPoId).isNotNull();
        ResponseEntity<Map> res = rest.exchange(
                PO_URL + "/" + createdPoId + "/issue", HttpMethod.PATCH,
                new HttpEntity<>(bearer(adminToken)), Map.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) res.getBody().get("data");
        assertThat(data.get("status")).isEqualTo("ISSUED");
        assertThat(data.get("issuedAt")).isNotNull();
    }

    @Test @Order(10)
    @DisplayName("PATCH /purchase-orders/{id}/issue: already ISSUED → 400")
    void issue_alreadyIssued_returns400() {
        ResponseEntity<Map> res = rest.exchange(
                PO_URL + "/" + createdPoId + "/issue", HttpMethod.PATCH,
                new HttpEntity<>(bearer(adminToken)), Map.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test @Order(11)
    @DisplayName("PUT /purchase-orders/{id}: ISSUED → 400 not editable")
    void update_issued_returns400() {
        var req = new UpdatePurchaseOrderRequest("GBP", null, null, null, null, null);
        ResponseEntity<Map> res = rest.exchange(
                PO_URL + "/" + createdPoId, HttpMethod.PUT,
                new HttpEntity<>(req, bearer(adminToken)), Map.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ── 6. Acknowledge ────────────────────────────────────────────────────────

    @Test @Order(12)
    @DisplayName("PATCH /purchase-orders/{id}/acknowledge: ISSUED → 200 ACKNOWLEDGED")
    void acknowledge_issued_returns200() {
        ResponseEntity<Map> res = rest.exchange(
                PO_URL + "/" + createdPoId + "/acknowledge", HttpMethod.PATCH,
                new HttpEntity<>(bearer(adminToken)), Map.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) res.getBody().get("data");
        assertThat(data.get("status")).isEqualTo("ACKNOWLEDGED");
        assertThat(data.get("acknowledgedAt")).isNotNull();
    }

    // ── 7. Record delivery ────────────────────────────────────────────────────

    @Test @Order(13)
    @DisplayName("PATCH /purchase-orders/{id}/deliver: partial → 200 PARTIALLY_DELIVERED")
    void deliver_partial_returns200() {
        // Fetch items to get the item id
        ResponseEntity<Map> getRes = rest.exchange(
                PO_URL + "/" + createdPoId, HttpMethod.GET,
                new HttpEntity<>(bearer(adminToken)), Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> poData = (Map<String, Object>) getRes.getBody().get("data");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) poData.get("items");
        Long itemId = Long.valueOf(items.get(0).get("id").toString());

        var req = new RecordDeliveryRequest(
                List.of(new RecordDeliveryRequest.DeliveryLine(itemId, new BigDecimal("2"))));

        ResponseEntity<Map> res = rest.exchange(
                PO_URL + "/" + createdPoId + "/deliver", HttpMethod.PATCH,
                new HttpEntity<>(req, bearer(adminToken)), Map.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) res.getBody().get("data");
        assertThat(data.get("status")).isEqualTo("PARTIALLY_DELIVERED");
    }

    @Test @Order(14)
    @DisplayName("PATCH /purchase-orders/{id}/deliver: full → 200 DELIVERED")
    void deliver_full_returns200() {
        ResponseEntity<Map> getRes = rest.exchange(
                PO_URL + "/" + createdPoId, HttpMethod.GET,
                new HttpEntity<>(bearer(adminToken)), Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> poData = (Map<String, Object>) getRes.getBody().get("data");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) poData.get("items");
        Long itemId = Long.valueOf(items.get(0).get("id").toString());

        // deliver remaining 3 (already delivered 2 out of 5)
        var req = new RecordDeliveryRequest(
                List.of(new RecordDeliveryRequest.DeliveryLine(itemId, new BigDecimal("3"))));

        ResponseEntity<Map> res = rest.exchange(
                PO_URL + "/" + createdPoId + "/deliver", HttpMethod.PATCH,
                new HttpEntity<>(req, bearer(adminToken)), Map.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) res.getBody().get("data");
        assertThat(data.get("status")).isEqualTo("DELIVERED");
        assertThat(data.get("deliveredAt")).isNotNull();
    }

    // ── 8. Cancel (new PO) ────────────────────────────────────────────────────

    @Test @Order(15)
    @DisplayName("PATCH /purchase-orders/{id}/cancel: DRAFT → 200 CANCELLED")
    void cancel_draft_returns200() {
        ResponseEntity<Map> createRes = rest.exchange(
                PO_URL, HttpMethod.POST,
                new HttpEntity<>(buildCreateReq(), bearer(adminToken)), Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> created = (Map<String, Object>) createRes.getBody().get("data");
        Long cancelId = Long.valueOf(created.get("id").toString());

        ResponseEntity<Map> res = rest.exchange(
                PO_URL + "/" + cancelId + "/cancel", HttpMethod.PATCH,
                new HttpEntity<>(bearer(adminToken)), Map.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) res.getBody().get("data");
        assertThat(data.get("status")).isEqualTo("CANCELLED");
    }

    @Test @Order(16)
    @DisplayName("PATCH /purchase-orders/{id}/cancel: ACKNOWLEDGED → 400 cannot cancel")
    void cancel_acknowledged_returns400() {
        ResponseEntity<Map> res = rest.exchange(
                PO_URL + "/" + createdPoId + "/cancel", HttpMethod.PATCH,
                new HttpEntity<>(bearer(adminToken)), Map.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ── 9. Get by PO number ───────────────────────────────────────────────────

    @Test @Order(17)
    @DisplayName("GET /purchase-orders/number/{poNumber}: existing → 200")
    void getByPoNumber_existing_returns200() {
        ResponseEntity<Map> getRes = rest.exchange(
                PO_URL + "/" + createdPoId, HttpMethod.GET,
                new HttpEntity<>(bearer(adminToken)), Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) getRes.getBody().get("data");
        String poNumber = data.get("poNumber").toString();

        ResponseEntity<Map> res = rest.exchange(
                PO_URL + "/number/" + poNumber, HttpMethod.GET,
                new HttpEntity<>(bearer(adminToken)), Map.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
