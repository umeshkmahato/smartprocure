package com.smartprocure.notification.consumer;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Centralised Dead-Letter-Topic monitor.
 *
 * <p>Listens to all three DLT topics with a dedicated consumer group
 * ({@code sp-notification-dlq-monitor}) so DLT messages are observable
 * in Grafana without being re-consumed by the main processing group.
 *
 * <p>Each @RetryableTopic consumer already has a typed {@code @DltHandler}
 * for event-specific recovery logic.  This component provides the ops-level
 * audit trail: every DLT message is logged at ERROR level and counted in
 * Micrometer so Grafana can fire an alert.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DlqConsumer {

    private final MeterRegistry meterRegistry;

    @KafkaListener(
            topics = {
                "sp.vendor.approved-dlt",
                "sp.po.issued-dlt",
                "sp.invoice.verified-dlt"
            },
            groupId          = "sp-notification-dlq-monitor",
            containerFactory = "vendorApprovedListenerFactory"   // string consumer — reuse factory
    )
    public void onDeadLetter(ConsumerRecord<String, ?> record) {
        log.error(
            "[DLQ] Unprocessable message: topic={}, partition={}, offset={}, key={}, payload={}",
            record.topic(), record.partition(), record.offset(),
            record.key(), record.value()
        );
        meterRegistry.counter("kafka.dlq.received", "topic", record.topic()).increment();
    }
}
