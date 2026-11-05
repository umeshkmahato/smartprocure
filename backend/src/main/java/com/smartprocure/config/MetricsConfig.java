package com.smartprocure.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Phase 14 — Custom Micrometer metrics for SmartProcure business events.
 *
 * <p>All counters share the {@code smartprocure.} prefix and carry an
 * {@code app} tag (injected by {@code management.metrics.tags.application}).
 *
 * <p>Counters are exposed via {@code /actuator/prometheus} and scraped by
 * Prometheus; the companion Grafana dashboard reads them by name.
 */
@Configuration
public class MetricsConfig {

    // ── in-flight gauges (atomic so reads/writes are thread-safe) ────────────
    private final AtomicInteger activePurchaseRequests  = new AtomicInteger(0);
    private final AtomicInteger pendingApprovals        = new AtomicInteger(0);
    private final AtomicInteger openQuotationRequests   = new AtomicInteger(0);

    // ═════════════════════════════════════════════════════════════════════════
    //  MeterBinder — registers every meter once the registry is ready
    // ═════════════════════════════════════════════════════════════════════════
    @Bean
    public MeterBinder smartProcureMetrics() {
        return registry -> {

            // ── Purchase Requests ─────────────────────────────────────────────
            purchaseRequestCreatedCounter(registry);
            purchaseRequestApprovedCounter(registry);
            purchaseRequestRejectedCounter(registry);
            purchaseRequestCancelledCounter(registry);

            // ── Approval Workflow ─────────────────────────────────────────────
            approvalDecisionCounter(registry, "approved");
            approvalDecisionCounter(registry, "rejected");
            approvalDelegationCreatedCounter(registry);

            // ── Quotations ────────────────────────────────────────────────────
            quotationRequestPublishedCounter(registry);
            quotationSubmittedCounter(registry);
            quotationSelectedCounter(registry);

            // ── Purchase Orders ───────────────────────────────────────────────
            purchaseOrderIssuedCounter(registry);
            purchaseOrderDeliveredCounter(registry);
            purchaseOrderCancelledCounter(registry);

            // ── Invoices ──────────────────────────────────────────────────────
            invoiceCreatedCounter(registry);
            invoiceVerifiedCounter(registry);
            invoiceDisputedCounter(registry);

            // ── Kafka Events ──────────────────────────────────────────────────
            kafkaEventPublishedCounter(registry);
            kafkaEventConsumedCounter(registry);
            kafkaEventDlqCounter(registry);

            // ── AI / Vector Search ────────────────────────────────────────────
            aiRecommendationRequestCounter(registry);
            aiChatRequestCounter(registry);
            vendorEmbeddingIndexedCounter(registry);

            // ── Security ──────────────────────────────────────────────────────
            authLoginSuccessCounter(registry);
            authLoginFailureCounter(registry);
            authTokenRefreshCounter(registry);

            // ── Gauges (live counts) ──────────────────────────────────────────
            Gauge.builder("smartprocure.purchase_requests.active",
                            activePurchaseRequests, AtomicInteger::get)
                    .description("Number of purchase requests currently in PENDING/APPROVED state")
                    .tag("module", "purchase-request")
                    .register(registry);

            Gauge.builder("smartprocure.approvals.pending",
                            pendingApprovals, AtomicInteger::get)
                    .description("Number of approval requests awaiting a decision")
                    .tag("module", "approval")
                    .register(registry);

            Gauge.builder("smartprocure.quotation_requests.open",
                            openQuotationRequests, AtomicInteger::get)
                    .description("Number of RFQs currently in PUBLISHED state")
                    .tag("module", "quotation")
                    .register(registry);
        };
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Individual @Bean counters — injected into services that increment them
    // ═════════════════════════════════════════════════════════════════════════

    // ── Purchase Requests ─────────────────────────────────────────────────────

    @Bean
    public Counter purchaseRequestCreatedCounter(MeterRegistry registry) {
        return Counter.builder("smartprocure.purchase_requests.created")
                .description("Total purchase requests created")
                .tag("module", "purchase-request")
                .register(registry);
    }

    @Bean
    public Counter purchaseRequestApprovedCounter(MeterRegistry registry) {
        return Counter.builder("smartprocure.purchase_requests.approved")
                .description("Total purchase requests that reached APPROVED status")
                .tag("module", "purchase-request")
                .register(registry);
    }

    @Bean
    public Counter purchaseRequestRejectedCounter(MeterRegistry registry) {
        return Counter.builder("smartprocure.purchase_requests.rejected")
                .description("Total purchase requests rejected")
                .tag("module", "purchase-request")
                .register(registry);
    }

    @Bean
    public Counter purchaseRequestCancelledCounter(MeterRegistry registry) {
        return Counter.builder("smartprocure.purchase_requests.cancelled")
                .description("Total purchase requests cancelled by requester")
                .tag("module", "purchase-request")
                .register(registry);
    }

    // ── Approval Workflow ─────────────────────────────────────────────────────

    @Bean
    public Counter approvalDelegationCreatedCounter(MeterRegistry registry) {
        return Counter.builder("smartprocure.approvals.delegation_created")
                .description("Total approval delegations created")
                .tag("module", "approval")
                .register(registry);
    }

    @Bean
    public Counter quotationRequestPublishedCounter(MeterRegistry registry) {
        return Counter.builder("smartprocure.quotations.rfq_published")
                .description("Total RFQs published to vendors")
                .tag("module", "quotation")
                .register(registry);
    }

    @Bean
    public Counter quotationSubmittedCounter(MeterRegistry registry) {
        return Counter.builder("smartprocure.quotations.submitted")
                .description("Total quotations submitted by vendors")
                .tag("module", "quotation")
                .register(registry);
    }

    @Bean
    public Counter quotationSelectedCounter(MeterRegistry registry) {
        return Counter.builder("smartprocure.quotations.selected")
                .description("Total winning quotations selected")
                .tag("module", "quotation")
                .register(registry);
    }

    // ── Purchase Orders ───────────────────────────────────────────────────────

    @Bean
    public Counter purchaseOrderIssuedCounter(MeterRegistry registry) {
        return Counter.builder("smartprocure.purchase_orders.issued")
                .description("Total purchase orders issued to vendors")
                .tag("module", "purchase-order")
                .register(registry);
    }

    @Bean
    public Counter purchaseOrderDeliveredCounter(MeterRegistry registry) {
        return Counter.builder("smartprocure.purchase_orders.delivered")
                .description("Total purchase orders with delivery recorded")
                .tag("module", "purchase-order")
                .register(registry);
    }

    @Bean
    public Counter purchaseOrderCancelledCounter(MeterRegistry registry) {
        return Counter.builder("smartprocure.purchase_orders.cancelled")
                .description("Total purchase orders cancelled")
                .tag("module", "purchase-order")
                .register(registry);
    }

    // ── Invoices ──────────────────────────────────────────────────────────────

    @Bean
    public Counter invoiceCreatedCounter(MeterRegistry registry) {
        return Counter.builder("smartprocure.invoices.created")
                .description("Total invoices uploaded")
                .tag("module", "invoice")
                .register(registry);
    }

    @Bean
    public Counter invoiceVerifiedCounter(MeterRegistry registry) {
        return Counter.builder("smartprocure.invoices.verified")
                .description("Total invoices verified and matched to PO")
                .tag("module", "invoice")
                .register(registry);
    }

    @Bean
    public Counter invoiceDisputedCounter(MeterRegistry registry) {
        return Counter.builder("smartprocure.invoices.disputed")
                .description("Total invoices raised in dispute")
                .tag("module", "invoice")
                .register(registry);
    }

    // ── Kafka Events ──────────────────────────────────────────────────────────

    @Bean
    public Counter kafkaEventPublishedCounter(MeterRegistry registry) {
        return Counter.builder("smartprocure.kafka.events_published")
                .description("Total domain events published to Kafka")
                .tag("module", "kafka")
                .register(registry);
    }

    @Bean
    public Counter kafkaEventConsumedCounter(MeterRegistry registry) {
        return Counter.builder("smartprocure.kafka.events_consumed")
                .description("Total domain events successfully consumed")
                .tag("module", "kafka")
                .register(registry);
    }

    @Bean
    public Counter kafkaEventDlqCounter(MeterRegistry registry) {
        return Counter.builder("smartprocure.kafka.events_dlq")
                .description("Total events routed to dead-letter queue")
                .tag("module", "kafka")
                .register(registry);
    }

    // ── AI / Vector Search ────────────────────────────────────────────────────

    @Bean
    public Counter aiRecommendationRequestCounter(MeterRegistry registry) {
        return Counter.builder("smartprocure.ai.recommendation_requests")
                .description("Total AI vendor recommendation requests")
                .tag("module", "ai")
                .register(registry);
    }

    @Bean
    public Counter aiChatRequestCounter(MeterRegistry registry) {
        return Counter.builder("smartprocure.ai.chat_requests")
                .description("Total AI procurement assistant chat requests")
                .tag("module", "ai")
                .register(registry);
    }

    @Bean
    public Counter vendorEmbeddingIndexedCounter(MeterRegistry registry) {
        return Counter.builder("smartprocure.ai.vendor_embeddings_indexed")
                .description("Total vendor embeddings written to pgvector")
                .tag("module", "ai")
                .register(registry);
    }

    // ── Security ──────────────────────────────────────────────────────────────

    @Bean
    public Counter authLoginSuccessCounter(MeterRegistry registry) {
        return Counter.builder("smartprocure.auth.login_success")
                .description("Total successful login attempts")
                .tag("module", "auth")
                .register(registry);
    }

    @Bean
    public Counter authLoginFailureCounter(MeterRegistry registry) {
        return Counter.builder("smartprocure.auth.login_failure")
                .description("Total failed login attempts")
                .tag("module", "auth")
                .register(registry);
    }

    @Bean
    public Counter authTokenRefreshCounter(MeterRegistry registry) {
        return Counter.builder("smartprocure.auth.token_refresh")
                .description("Total JWT refresh-token exchanges")
                .tag("module", "auth")
                .register(registry);
    }

    // ── Timers ────────────────────────────────────────────────────────────────

    @Bean
    public Timer aiRecommendationTimer(MeterRegistry registry) {
        return Timer.builder("smartprocure.ai.recommendation_latency")
                .description("End-to-end latency for AI vendor recommendation (pgvector similarity search + ranking)")
                .tag("module", "ai")
                .publishPercentiles(0.5, 0.95, 0.99)
                .publishPercentileHistogram()
                .register(registry);
    }

    @Bean
    public Timer aiChatTimer(MeterRegistry registry) {
        return Timer.builder("smartprocure.ai.chat_latency")
                .description("End-to-end latency for AI chat response from Ollama")
                .tag("module", "ai")
                .publishPercentiles(0.5, 0.95, 0.99)
                .publishPercentileHistogram()
                .register(registry);
    }

    @Bean
    public Timer approvalDecisionTimer(MeterRegistry registry) {
        return Timer.builder("smartprocure.approvals.decision_latency")
                .description("Time taken to process an approval decision")
                .tag("module", "approval")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);
    }

    // ── Gauge accessors (called by services to update live counts) ────────────

    public AtomicInteger getActivePurchaseRequests()  { return activePurchaseRequests; }
    public AtomicInteger getPendingApprovals()         { return pendingApprovals; }
    public AtomicInteger getOpenQuotationRequests()    { return openQuotationRequests; }

    // ── Private helpers used inside MeterBinder lambda ────────────────────────

    private void approvalDecisionCounter(MeterRegistry registry, String decision) {
        Counter.builder("smartprocure.approvals.decisions")
                .description("Total approval decisions made")
                .tag("module", "approval")
                .tag("decision", decision)
                .register(registry);
    }
}
