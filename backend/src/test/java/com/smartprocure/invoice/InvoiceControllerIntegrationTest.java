package com.smartprocure.invoice;

import com.smartprocure.invoice.dto.*;
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
@DisplayName("Invoice endpoints — integration")
class InvoiceControllerIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("pgvector/pgvector:pg16")
                    .withDatabaseName("smartprocure_inv_it")
                    .withUsername("sp_it")
                    .withPassword("sp_it_pass");

    @Autowired private TestRestTemplate rest;

    private static String adminToken;
    private static Long   vendorId;
    private static Long   poId;
    private static Long   createdInvoiceId;

    private static final String INV_URL       = "/api/v1/invoices";
    private static final String PO_URL        = "/api/v1/purchase-orders";
    private static final String VENDOR_URL    = "/api/v1/vendors";
    private static final String AUTH_REGISTER = "/api/v1/auth/register";
    private static final String AUTH_LOGIN    = "/api/v1/auth/login";

    // ── Bootstrap ─────────────────────────────────────────────────────────────

    @BeforeEach
    void ensureBootstrap() {
        if (adminToken != null) return;
        adminToken = registerAndLogin("inv_admin", "inv_admin@sp.test", "Admin@Pass1!");
        vendorId   = createAndApproveVendor();
        poId       = createPoAndDeliver();
    }

    private String registerAndLogin(String username, String email, String password) {
        var reg = new RegisterRequest(username, email, password, "Inv", "Admin", "Finance", null);
        rest.postForEntity(AUTH_REGISTER, reg, Map.class);
        ResponseEntity<Map> res = rest.postForEntity(
                AUTH_LOGIN, new LoginRequest(username, password), Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) res.getBody().get("data");
        return (String) data.get("accessToken");
    }

    private Long createAndApproveVendor() {
        var req = new CreateVendorRequest(
                "Invoice Vendor", "inv_vendor@vendor.test",
                "+1-555-0200", "789 Vendor Rd", "Boston", "USA",
                "TAX-INV-001", "REG-INV-001",
                "IT Equipment", "USD", null);
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

    private Long createPoAndDeliver() {
        // Create PO
        var createReq = new CreatePurchaseOrderRequest(
                vendorId, null, null, "USD",
                LocalDate.now().plusDays(14),
                "1 Finance St", "Net 30", "Invoice IT PO",
                List.of(new PoLineItemRequest("Server", "Rack unit",
                        new BigDecimal("2"), "unit", new BigDecimal("500.00"), null)));
        ResponseEntity<Map> createRes = rest.exchange(
                PO_URL, HttpMethod.POST,
                new HttpEntity<>(createReq, bearer(adminToken)), Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> poData = (Map<String, Object>) createRes.getBody().get("data");
        Long id = Long.valueOf(poData.get("id").toString());

        // Issue → Acknowledge → Deliver (full)
        rest.exchange(PO_URL + "/" + id + "/issue", HttpMethod.PATCH,
                new HttpEntity<>(bearer(adminToken)), Map.class);
        rest.exchange(PO_URL + "/" + id + "/acknowledge", HttpMethod.PATCH,
                new HttpEntity<>(bearer(adminToken)), Map.class);

        // Fetch items to get itemId
        ResponseEntity<Map> getRes = rest.exchange(
                PO_URL + "/" + id, HttpMethod.GET,
                new HttpEntity<>(bearer(adminToken)), Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> fetchedPo = (Map<String, Object>) getRes.getBody().get("data");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) fetchedPo.get("items");
        Long itemId = Long.valueOf(items.get(0).get("id").toString());

        var deliverReq = new RecordDeliveryRequest(
                List.of(new RecordDeliveryRequest.DeliveryLine(itemId, new BigDecimal("2"))));
        rest.exchange(PO_URL + "/" + id + "/deliver", HttpMethod.PATCH,
                new HttpEntity<>(deliverReq, bearer(adminToken)), Map.class);
        return id;
    }

    private HttpHeaders bearer(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    private CreateInvoiceRequest buildCreateReq() {
        return new CreateInvoiceRequest(
                poId, "INV-IT-" + System.nanoTime(),
                new BigDecimal("1000.00"), "USD",
                LocalDate.now(), LocalDate.now().plusDays(30),
                "https://storage.example.com/inv001.pdf", "Integration test invoice");
    }

    // ── 1. Create ─────────────────────────────────────────────────────────────

    @Test @Order(1)
    @DisplayName("POST /invoices: valid → 201 SUBMITTED invoice")
    void create_valid_returns201() {
        ResponseEntity<Map> res = rest.exchange(
                INV_URL, HttpMethod.POST,
                new HttpEntity<>(buildCreateReq(), bearer(adminToken)), Map.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) res.getBody().get("data");
        assertThat(data.get("status")).isEqualTo("SUBMITTED");
        assertThat(data.get("purchaseOrderId").toString()).isEqualTo(poId.toString());
        createdInvoiceId = Long.valueOf(data.get("id").toString());
    }

    @Test @Order(2)
    @DisplayName("POST /invoices: duplicate invoice number → 409")
    void create_duplicateNumber_returns409() {
        // first call already stored; extract the number from the first invoice
        ResponseEntity<Map> getRes = rest.exchange(
                INV_URL + "/" + createdInvoiceId, HttpMethod.GET,
                new HttpEntity<>(bearer(adminToken)), Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> existing = (Map<String, Object>) getRes.getBody().get("data");
        String existingNumber = existing.get("invoiceNumber").toString();

        var dupReq = new CreateInvoiceRequest(poId, existingNumber,
                new BigDecimal("1000.00"), "USD", LocalDate.now(), null, null, null);

        ResponseEntity<Map> res = rest.exchange(
                INV_URL, HttpMethod.POST,
                new HttpEntity<>(dupReq, bearer(adminToken)), Map.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test @Order(3)
    @DisplayName("POST /invoices: unauthenticated → 401")
    void create_unauthenticated_returns401() {
        ResponseEntity<Map> res = rest.postForEntity(INV_URL, buildCreateReq(), Map.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ── 2. Get ────────────────────────────────────────────────────────────────

    @Test @Order(4)
    @DisplayName("GET /invoices/{id}: existing → 200 with DTO")
    void getById_existing_returns200() {
        assertThat(createdInvoiceId).isNotNull();
        ResponseEntity<Map> res = rest.exchange(
                INV_URL + "/" + createdInvoiceId, HttpMethod.GET,
                new HttpEntity<>(bearer(adminToken)), Map.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) res.getBody().get("data");
        assertThat(data.get("id").toString()).isEqualTo(createdInvoiceId.toString());
        assertThat(data.get("poNumber")).isNotNull();
        assertThat(data.get("vendorName")).isNotNull();
    }

    @Test @Order(5)
    @DisplayName("GET /invoices/{id}: non-existent → 404")
    void getById_notFound_returns404() {
        ResponseEntity<Map> res = rest.exchange(
                INV_URL + "/999999", HttpMethod.GET,
                new HttpEntity<>(bearer(adminToken)), Map.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── 3. Search ─────────────────────────────────────────────────────────────

    @Test @Order(6)
    @DisplayName("GET /invoices: paginated search → 200")
    void search_returns200() {
        ResponseEntity<Map> res = rest.exchange(
                INV_URL + "?page=0&size=10", HttpMethod.GET,
                new HttpEntity<>(bearer(adminToken)), Map.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) res.getBody().get("data");
        assertThat(data).containsKey("content");
        assertThat(data).containsKey("totalElements");
    }

    @Test @Order(7)
    @DisplayName("GET /invoices: filter by status=SUBMITTED → 200")
    void search_filterByStatus_returns200() {
        ResponseEntity<Map> res = rest.exchange(
                INV_URL + "?status=SUBMITTED&page=0&size=10", HttpMethod.GET,
                new HttpEntity<>(bearer(adminToken)), Map.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // ── 4. Update ─────────────────────────────────────────────────────────────

    @Test @Order(8)
    @DisplayName("PUT /invoices/{id}: SUBMITTED → 200 updated amount")
    void update_submitted_returns200() {
        assertThat(createdInvoiceId).isNotNull();
        var req = new UpdateInvoiceRequest(new BigDecimal("990.00"),
                null, null, null, null, "Revised total");

        ResponseEntity<Map> res = rest.exchange(
                INV_URL + "/" + createdInvoiceId, HttpMethod.PUT,
                new HttpEntity<>(req, bearer(adminToken)), Map.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) res.getBody().get("data");
        // restore to 1000 for verify test
        rest.exchange(INV_URL + "/" + createdInvoiceId, HttpMethod.PUT,
                new HttpEntity<>(new UpdateInvoiceRequest(new BigDecimal("1000.00"),
                        null, null, null, null, null), bearer(adminToken)), Map.class);
        assertThat(data.get("notes")).isEqualTo("Revised total");
    }

    // ── 5. Review ─────────────────────────────────────────────────────────────

    @Test @Order(9)
    @DisplayName("PATCH /invoices/{id}/review: SUBMITTED → 200 UNDER_REVIEW")
    void review_submitted_returns200() {
        ResponseEntity<Map> res = rest.exchange(
                INV_URL + "/" + createdInvoiceId + "/review", HttpMethod.PATCH,
                new HttpEntity<>(bearer(adminToken)), Map.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) res.getBody().get("data");
        assertThat(data.get("status")).isEqualTo("UNDER_REVIEW");
    }

    // ── 6. Verify ─────────────────────────────────────────────────────────────

    @Test @Order(10)
    @DisplayName("PATCH /invoices/{id}/verify: UNDER_REVIEW + amount match → 200 VERIFIED")
    void verify_underReview_returns200() {
        ResponseEntity<Map> res = rest.exchange(
                INV_URL + "/" + createdInvoiceId + "/verify", HttpMethod.PATCH,
                new HttpEntity<>(bearer(adminToken)), Map.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) res.getBody().get("data");
        assertThat(data.get("status")).isEqualTo("VERIFIED");
        assertThat(data.get("verifiedAt")).isNotNull();
    }

    @Test @Order(11)
    @DisplayName("PATCH /invoices/{id}/verify: already VERIFIED → 400")
    void verify_alreadyVerified_returns400() {
        ResponseEntity<Map> res = rest.exchange(
                INV_URL + "/" + createdInvoiceId + "/verify", HttpMethod.PATCH,
                new HttpEntity<>(bearer(adminToken)), Map.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ── 7. Pay ────────────────────────────────────────────────────────────────

    @Test @Order(12)
    @DisplayName("PATCH /invoices/{id}/pay: VERIFIED → 200 PAID")
    void pay_verified_returns200() {
        ResponseEntity<Map> res = rest.exchange(
                INV_URL + "/" + createdInvoiceId + "/pay", HttpMethod.PATCH,
                new HttpEntity<>(bearer(adminToken)), Map.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) res.getBody().get("data");
        assertThat(data.get("status")).isEqualTo("PAID");
        assertThat(data.get("paidAt")).isNotNull();
    }

    // ── 8. Dispute flow (fresh invoice) ───────────────────────────────────────

    @Test @Order(13)
    @DisplayName("PATCH /invoices/{id}/dispute: SUBMITTED → 200 DISPUTED with reason")
    void dispute_submitted_returns200() {
        // Submit a fresh invoice on the same (now INVOICED) PO — reuse by creating on new PO
        // Create a second PO + deliver it for dispute test
        Long disputePoId = createPoAndDeliver();
        var createReq = new CreateInvoiceRequest(disputePoId, "INV-DISPUTE-" + System.nanoTime(),
                new BigDecimal("1000.00"), "USD", LocalDate.now(), null, null, null);
        ResponseEntity<Map> createRes = rest.exchange(
                INV_URL, HttpMethod.POST,
                new HttpEntity<>(createReq, bearer(adminToken)), Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> created = (Map<String, Object>) createRes.getBody().get("data");
        Long disputeInvId = Long.valueOf(created.get("id").toString());

        var disputeReq = new DisputeInvoiceRequest("Tax amount is incorrect");
        ResponseEntity<Map> res = rest.exchange(
                INV_URL + "/" + disputeInvId + "/dispute", HttpMethod.PATCH,
                new HttpEntity<>(disputeReq, bearer(adminToken)), Map.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) res.getBody().get("data");
        assertThat(data.get("status")).isEqualTo("DISPUTED");
        assertThat(data.get("disputeReason")).isEqualTo("Tax amount is incorrect");
    }

    // ── 9. Cancel ─────────────────────────────────────────────────────────────

    @Test @Order(14)
    @DisplayName("PATCH /invoices/{id}/cancel: SUBMITTED → 200 CANCELLED")
    void cancel_submitted_returns200() {
        Long cancelPoId = createPoAndDeliver();
        var createReq = new CreateInvoiceRequest(cancelPoId, "INV-CANCEL-" + System.nanoTime(),
                new BigDecimal("1000.00"), "USD", LocalDate.now(), null, null, null);
        ResponseEntity<Map> createRes = rest.exchange(
                INV_URL, HttpMethod.POST,
                new HttpEntity<>(createReq, bearer(adminToken)), Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> created = (Map<String, Object>) createRes.getBody().get("data");
        Long cancelInvId = Long.valueOf(created.get("id").toString());

        ResponseEntity<Map> res = rest.exchange(
                INV_URL + "/" + cancelInvId + "/cancel", HttpMethod.PATCH,
                new HttpEntity<>(bearer(adminToken)), Map.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) res.getBody().get("data");
        assertThat(data.get("status")).isEqualTo("CANCELLED");
    }

    @Test @Order(15)
    @DisplayName("PATCH /invoices/{id}/cancel: PAID → 400 cannot cancel")
    void cancel_paid_returns400() {
        ResponseEntity<Map> res = rest.exchange(
                INV_URL + "/" + createdInvoiceId + "/cancel", HttpMethod.PATCH,
                new HttpEntity<>(bearer(adminToken)), Map.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
