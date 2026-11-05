package com.smartprocure.events;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Typed Kafka event publisher for all SmartProcure domain events.
 *
 * <p>Each publish method:
 * <ul>
 *   <li>Uses the {@code eventId} (UUID) as the Kafka message key — ensures
 *       events for the same aggregate land on the same partition.</li>
 *   <li>Increments a Micrometer counter tagged with {@code event_type}.</li>
 *   <li>Logs success/failure without blocking the calling thread (async callback).</li>
 * </ul>
 *
 * <p>All Kafka producer settings (acks=all, retries=3) are defined in
 * {@code application.yml} and are inherited by this template.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaEventPublisher {

    static final String TOPIC_VENDOR_APPROVED  = "sp.vendor.approved";
    static final String TOPIC_PO_ISSUED        = "sp.po.issued";
    static final String TOPIC_INVOICE_VERIFIED = "sp.invoice.verified";

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final MeterRegistry                 meterRegistry;

    // ── Public API ────────────────────────────────────────────────────────────

    public void publishVendorApproved(VendorApprovedEvent event) {
        send(TOPIC_VENDOR_APPROVED, event.eventId(), event, "vendor.approved");
    }

    public void publishPoIssued(PurchaseOrderIssuedEvent event) {
        send(TOPIC_PO_ISSUED, event.eventId(), event, "po.issued");
    }

    public void publishInvoiceVerified(InvoiceVerifiedEvent event) {
        send(TOPIC_INVOICE_VERIFIED, event.eventId(), event, "invoice.verified");
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private void send(String topic, String key, Object payload, String metricTag) {
        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(topic, key, payload);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("[Kafka] Failed to publish event: topic={}, key={}, error={}",
                        topic, key, ex.getMessage(), ex);
                meterRegistry.counter("kafka.events.published",
                        "topic", topic, "event_type", metricTag, "status", "error").increment();
            } else {
                log.info("[Kafka] Event published: topic={}, key={}, partition={}, offset={}",
                        topic, key,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
                meterRegistry.counter("kafka.events.published",
                        "topic", topic, "event_type", metricTag, "status", "success").increment();
            }
        });
    }
}
