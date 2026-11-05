package com.smartprocure.config;

import com.smartprocure.events.KafkaEventPublisher;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Declares all Kafka topics used by SmartProcure.
 *
 * <p>Main topics have 3 partitions and replication-factor 1 (dev default;
 * set KAFKA_REPLICATION_FACTOR=3 in production via broker defaults).
 * DLT topics have 1 partition — failed messages are low-volume and must
 * be processed in order for ops visibility.
 *
 * <p>Topics are created idempotently on startup via the Kafka AdminClient.
 * If a topic already exists with the same configuration, the call is a no-op.
 */
@Configuration
public class KafkaTopicConfig {

    private static final int PARTITIONS   = 3;
    private static final int REPLICAS     = 1;
    private static final int DLT_PARTS    = 1;

    // ── Main topics ───────────────────────────────────────────────────────────

    @Bean
    public NewTopic vendorApprovedTopic() {
        return TopicBuilder.name(KafkaEventPublisher.TOPIC_VENDOR_APPROVED)
                .partitions(PARTITIONS)
                .replicas(REPLICAS)
                .build();
    }

    @Bean
    public NewTopic poIssuedTopic() {
        return TopicBuilder.name(KafkaEventPublisher.TOPIC_PO_ISSUED)
                .partitions(PARTITIONS)
                .replicas(REPLICAS)
                .build();
    }

    @Bean
    public NewTopic invoiceVerifiedTopic() {
        return TopicBuilder.name(KafkaEventPublisher.TOPIC_INVOICE_VERIFIED)
                .partitions(PARTITIONS)
                .replicas(REPLICAS)
                .build();
    }

    // ── Dead Letter Topics (DLT) ──────────────────────────────────────────────
    // @RetryableTopic appends "-dlt" by default; we pre-create them here so
    // they exist with known retention settings even before first failure.

    @Bean
    public NewTopic vendorApprovedDlt() {
        return TopicBuilder.name(KafkaEventPublisher.TOPIC_VENDOR_APPROVED + "-dlt")
                .partitions(DLT_PARTS)
                .replicas(REPLICAS)
                .build();
    }

    @Bean
    public NewTopic poIssuedDlt() {
        return TopicBuilder.name(KafkaEventPublisher.TOPIC_PO_ISSUED + "-dlt")
                .partitions(DLT_PARTS)
                .replicas(REPLICAS)
                .build();
    }

    @Bean
    public NewTopic invoiceVerifiedDlt() {
        return TopicBuilder.name(KafkaEventPublisher.TOPIC_INVOICE_VERIFIED + "-dlt")
                .partitions(DLT_PARTS)
                .replicas(REPLICAS)
                .build();
    }
}
