package com.smartprocure.notification.consumer;

import com.smartprocure.notification.model.VendorApprovedEvent;
import com.smartprocure.notification.service.IdempotencyService;
import com.smartprocure.notification.service.NotificationEmailService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class VendorEventConsumer {

    private final NotificationEmailService emailService;
    private final IdempotencyService       idempotencyService;
    private final MeterRegistry            meterRegistry;

    @RetryableTopic(
            attempts          = "4",
            backoff           = @Backoff(delay = 1_000, multiplier = 2.0, maxDelay = 8_000),
            autoCreateTopics  = "false",
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE
    )
    @KafkaListener(
            topics           = "sp.vendor.approved",
            containerFactory = "vendorApprovedListenerFactory"
    )
    public void onVendorApproved(
            VendorApprovedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET)         long   offset) {

        log.info("[VendorConsumer] Received: eventId={}, vendorId={}, topic={}, offset={}",
                event.eventId(), event.vendorId(), topic, offset);

        if (!idempotencyService.tryMarkProcessed(event.eventId())) {
            return; // duplicate — already processed
        }

        try {
            emailService.sendVendorApproved(event);
            meterRegistry.counter("kafka.events.consumed",
                    "topic", topic, "status", "success").increment();
        } catch (Exception ex) {
            meterRegistry.counter("kafka.events.consumed",
                    "topic", topic, "status", "error").increment();
            throw ex; // rethrow so @RetryableTopic can retry
        }
    }

    @DltHandler
    public void onVendorApprovedDlt(
            VendorApprovedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {

        log.error("[VendorConsumer] DLT: exhausted retries — eventId={}, vendorId={}",
                event.eventId(), event.vendorId());
        meterRegistry.counter("kafka.events.dlt", "topic", topic).increment();
    }
}
