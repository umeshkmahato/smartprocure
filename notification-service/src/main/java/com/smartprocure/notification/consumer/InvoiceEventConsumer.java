package com.smartprocure.notification.consumer;

import com.smartprocure.notification.model.InvoiceVerifiedEvent;
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
public class InvoiceEventConsumer {

    private final NotificationEmailService emailService;
    private final IdempotencyService       idempotencyService;
    private final MeterRegistry            meterRegistry;

    @RetryableTopic(
            attempts               = "4",
            backoff                = @Backoff(delay = 1_000, multiplier = 2.0, maxDelay = 8_000),
            autoCreateTopics       = "false",
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE
    )
    @KafkaListener(
            topics           = "sp.invoice.verified",
            containerFactory = "invoiceVerifiedListenerFactory"
    )
    public void onInvoiceVerified(
            InvoiceVerifiedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET)         long   offset) {

        log.info("[InvoiceConsumer] Received: eventId={}, invoiceId={}, topic={}, offset={}",
                event.eventId(), event.invoiceId(), topic, offset);

        if (!idempotencyService.tryMarkProcessed(event.eventId())) {
            return;
        }

        try {
            emailService.sendInvoiceVerified(event);
            meterRegistry.counter("kafka.events.consumed",
                    "topic", topic, "status", "success").increment();
        } catch (Exception ex) {
            meterRegistry.counter("kafka.events.consumed",
                    "topic", topic, "status", "error").increment();
            throw ex;
        }
    }

    @DltHandler
    public void onInvoiceVerifiedDlt(
            InvoiceVerifiedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {

        log.error("[InvoiceConsumer] DLT: exhausted retries — eventId={}, invoiceId={}",
                event.eventId(), event.invoiceId());
        meterRegistry.counter("kafka.events.dlt", "topic", topic).increment();
    }
}
