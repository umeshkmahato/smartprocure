package com.smartprocure.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.retry.annotation.EnableRetry;

/**
 * SmartProcure Notification Service.
 *
 * <p>Consumes domain events from Kafka and dispatches email notifications.
 * Guarantees at-least-once delivery with idempotency protection via Redis.
 *
 * <p>Topics consumed:
 * <ul>
 *   <li>{@code sp.vendor.approved}   → vendor approval email</li>
 *   <li>{@code sp.po.issued}         → purchase order issued email</li>
 *   <li>{@code sp.invoice.verified}  → invoice verified email</li>
 * </ul>
 */
@SpringBootApplication
@EnableKafka
@EnableRetry
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
