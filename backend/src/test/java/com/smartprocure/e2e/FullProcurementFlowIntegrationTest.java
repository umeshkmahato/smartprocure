package com.smartprocure.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Phase 16 — Full end-to-end procurement flow integration test.
 *
 * <p>Covers the complete happy path from user registration through invoice
 * verification, exercising every major module in a single ordered test suite
 * backed by real Testcontainer instances (Postgres, Kafka, Redis).
 *
 * <p>Test order:
 * <ol>
 *   <li>Register officer + approver accounts</li>
 *   <li>Login both accounts → capture JWTs</li>
 *   <li>Create Purchase Request with line items</li>
 *   <li>Initiate Approval Workflow</li>
 *   <li>Approve the PR (manager account)</li>
 *   <li>Create RFQ linked to the PR</li>
 *   <li>Submit Vendor Quotation</li>
 *   <li>Issue Purchase Order</li>
 *   <li>Submit Invoice</li>
 *   <li>Verify Invoice → assert VERIFIED status</li>
 * </ol>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Full Procurement Flow — E2E Integration Test")
class FullProcurementFlowIntegrationTest {

    // ── Testcontainers ────────────────────────────────────────────────────────

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("smartprocure_test")
                    .withUsername("sp_test")
                    .withPassword("sp_test");

    @Container
    static final KafkaContainer KAFKA =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    @SuppressWarnings("resource")
    @Container
    static final GenericContainer<?> REDIS =
            new GenericContainer<>("redis:7.2-alpine").withExposedPorts(6379);

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry reg) {
        reg.add("spring.datasource.url",      POSTGRES::getJdbcUrl);
        reg.add("spring.datasource.username",  POSTGRES::getUsername);
        reg.add("spring.datasource.password",  POSTGRES::getPassword);
        reg.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        reg.add("spring.data.redis.host",      () -> REDIS.getHost());
        reg.add("spring.data.redis.port",      () -> REDIS.getMappedPort(6379));
    }

    // ── Shared state (passed between ordered tests) ───────────────────────────

    private static String officerToken;
    private static String approverToken;
    private static Long   prId;
    private static Long   approvalId;
    private static Long   rfqId;
    private static Long   poId;
    private static Long   invoiceId;
    private static Long   vendorId = 1L;   // seeded by Flyway test migration

    @Autowired MockMvc       mvc;
    @Autowired ObjectMapper  om;

    // ─────────────────────────────────────────────────────────────────────────
    //  Auth
    // ─────────────────────────────────────────────────────────────────────────

    @Test @Order(1)
    @DisplayName("1 · Register procurement officer account")
    void registerOfficer() throws Exception {
        mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Alice",
                                  "lastName":  "Officer",
                                  "email":     "alice.e2e@smartprocure.dev",
                                  "password":  "Test@1234!",
                                  "role":      "PROCUREMENT_OFFICER"
                                }"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.email").value("alice.e2e@smartprocure.dev"));
    }

    @Test @Order(2)
    @DisplayName("2 · Register manager / approver account")
    void registerApprover() throws Exception {
        mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Bob",
                                  "lastName":  "Manager",
                                  "email":     "bob.e2e@smartprocure.dev",
                                  "password":  "Test@1234!",
                                  "role":      "MANAGER"
                                }"""))
                .andExpect(status().isCreated());
    }

    @Test @Order(3)
    @DisplayName("3 · Login officer → capture JWT")
    void loginOfficer() throws Exception {
        MvcResult res = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"alice.e2e@smartprocure.dev","password":"Test@1234!"}"""))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = om.readTree(res.getResponse().getContentAsString());
        officerToken = body.at("/data/accessToken").asText();
        assertThat(officerToken).isNotBlank();
    }

    @Test @Order(4)
    @DisplayName("4 · Login approver → capture JWT")
    void loginApprover() throws Exception {
        MvcResult res = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"bob.e2e@smartprocure.dev","password":"Test@1234!"}"""))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = om.readTree(res.getResponse().getContentAsString());
        approverToken = body.at("/data/accessToken").asText();
        assertThat(approverToken).isNotBlank();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Purchase Request
    // ─────────────────────────────────────────────────────────────────────────

    @Test @Order(5)
    @DisplayName("5 · Create Purchase Request with two line items")
    void createPurchaseRequest() throws Exception {
        MvcResult res = mvc.perform(post("/api/v1/purchase-requests")
                        .header("Authorization", "Bearer " + officerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "E2E — Server Infrastructure Upgrade",
                                  "description": "Full E2E test purchase request",
                                  "requiredByDate": "2025-12-31",
                                  "items": [
                                    {
                                      "description": "Server rack",
                                      "quantity": 2,
                                      "estimatedUnitPrice": 2500.00,
                                      "unit": "PCS"
                                    },
                                    {
                                      "description": "Network switch",
                                      "quantity": 4,
                                      "estimatedUnitPrice": 800.00,
                                      "unit": "PCS"
                                    }
                                  ]
                                }"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andReturn();

        prId = om.readTree(res.getResponse().getContentAsString()).at("/data/id").asLong();
        assertThat(prId).isPositive();
    }

    @Test @Order(6)
    @DisplayName("6 · GET /purchase-requests/{id} returns the created PR")
    void getPurchaseRequest() throws Exception {
        mvc.perform(get("/api/v1/purchase-requests/{id}", prId)
                        .header("Authorization", "Bearer " + officerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(prId))
                .andExpect(jsonPath("$.data.title").value("E2E — Server Infrastructure Upgrade"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Approval Workflow
    // ─────────────────────────────────────────────────────────────────────────

    @Test @Order(7)
    @DisplayName("7 · Initiate approval workflow for the PR")
    void initiateApproval() throws Exception {
        MvcResult res = mvc.perform(
                        post("/api/v1/approvals/purchase-requests/{prId}/initiate", prId)
                                .header("Authorization", "Bearer " + officerToken)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode steps = om.readTree(res.getResponse().getContentAsString()).at("/data");
        assertThat(steps.isArray()).isTrue();
        assertThat(steps.size()).isGreaterThan(0);
        approvalId = steps.get(0).at("/id").asLong();
    }

    @Test @Order(8)
    @DisplayName("8 · Approver sees PR in pending queue")
    void approverPendingQueue() throws Exception {
        mvc.perform(get("/api/v1/approvals/my/pending?page=0&size=20")
                        .header("Authorization", "Bearer " + approverToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test @Order(9)
    @DisplayName("9 · Approve the PR → status becomes APPROVED")
    void approvePr() throws Exception {
        mvc.perform(post("/api/v1/approvals/{id}/decide", approvalId)
                        .header("Authorization", "Bearer " + approverToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "decision": "APPROVED",
                                  "comment":  "Approved by E2E integration test"
                                }"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Quotation / RFQ
    // ─────────────────────────────────────────────────────────────────────────

    @Test @Order(10)
    @DisplayName("10 · Create RFQ linked to the approved PR")
    void createRfq() throws Exception {
        MvcResult res = mvc.perform(post("/api/v1/rfqs")
                        .header("Authorization", "Bearer " + officerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "purchaseRequestId":  %d,
                                  "title":              "E2E RFQ — Server Infrastructure",
                                  "description":        "E2E integration test RFQ",
                                  "submissionDeadline": "2025-11-30T23:59:59",
                                  "currency":           "USD"
                                }""".formatted(prId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn();

        rfqId = om.readTree(res.getResponse().getContentAsString()).at("/data/id").asLong();
        assertThat(rfqId).isPositive();
    }

    @Test @Order(11)
    @DisplayName("11 · Submit vendor quotation for the RFQ")
    void submitQuotation() throws Exception {
        mvc.perform(post("/api/v1/rfqs/{rfqId}/quotations/{vendorId}", rfqId, vendorId)
                        .header("Authorization", "Bearer " + officerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currency":     "USD",
                                  "validUntil":   "2025-12-15",
                                  "deliveryDays": 14,
                                  "notes":        "E2E test quotation",
                                  "lineItems": [
                                    {
                                      "description": "Server rack",
                                      "quantity": 2,
                                      "unitPrice": 2400.00,
                                      "unit": "PCS"
                                    },
                                    {
                                      "description": "Network switch",
                                      "quantity": 4,
                                      "unitPrice":  780.00,
                                      "unit": "PCS"
                                    }
                                  ]
                                }"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.vendorId").value(vendorId));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Purchase Order
    // ─────────────────────────────────────────────────────────────────────────

    @Test @Order(12)
    @DisplayName("12 · Issue Purchase Order to the winning vendor")
    void issuePurchaseOrder() throws Exception {
        MvcResult res = mvc.perform(post("/api/v1/purchase-orders")
                        .header("Authorization", "Bearer " + officerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "purchaseRequestId":    %d,
                                  "vendorId":             %d,
                                  "currency":             "USD",
                                  "deliveryAddress":      "123 Procurement St, New York, NY 10001",
                                  "requiredDeliveryDate": "2026-01-15",
                                  "notes":                "E2E integration test PO"
                                }""".formatted(prId, vendorId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.status").value("ISSUED"))
                .andReturn();

        poId = om.readTree(res.getResponse().getContentAsString()).at("/data/id").asLong();
        assertThat(poId).isPositive();
    }

    @Test @Order(13)
    @DisplayName("13 · GET /purchase-orders/{id} returns ISSUED order")
    void getPurchaseOrder() throws Exception {
        mvc.perform(get("/api/v1/purchase-orders/{id}", poId)
                        .header("Authorization", "Bearer " + officerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ISSUED"))
                .andExpect(jsonPath("$.data.vendorId").value(vendorId));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Invoice
    // ─────────────────────────────────────────────────────────────────────────

    @Test @Order(14)
    @DisplayName("14 · Vendor submits invoice against the PO")
    void submitInvoice() throws Exception {
        MvcResult res = mvc.perform(post("/api/v1/invoices")
                        .header("Authorization", "Bearer " + officerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "purchaseOrderId": %d,
                                  "vendorId":        %d,
                                  "invoiceNumber":   "INV-E2E-001",
                                  "invoiceDate":     "2026-01-10",
                                  "dueDate":         "2026-02-10",
                                  "currency":        "USD",
                                  "lineItems": [
                                    { "description": "Server rack",    "quantity": 2, "unitPrice": 2400.00 },
                                    { "description": "Network switch", "quantity": 4, "unitPrice":  780.00 }
                                  ]
                                }""".formatted(poId, vendorId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn();

        invoiceId = om.readTree(res.getResponse().getContentAsString()).at("/data/id").asLong();
        assertThat(invoiceId).isPositive();
    }

    @Test @Order(15)
    @DisplayName("15 · Finance verifies the invoice → VERIFIED")
    void verifyInvoice() throws Exception {
        mvc.perform(post("/api/v1/invoices/{id}/verify", invoiceId)
                        .header("Authorization", "Bearer " + officerToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("VERIFIED"));
    }

    @Test @Order(16)
    @DisplayName("16 · GET /invoices/{id} confirms final VERIFIED state")
    void getVerifiedInvoice() throws Exception {
        mvc.perform(get("/api/v1/invoices/{id}", invoiceId)
                        .header("Authorization", "Bearer " + officerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("VERIFIED"))
                .andExpect(jsonPath("$.data.invoiceNumber").value("INV-E2E-001"));
    }
}
