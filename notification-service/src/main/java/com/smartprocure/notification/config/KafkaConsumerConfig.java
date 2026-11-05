package com.smartprocure.notification.config;

import com.smartprocure.notification.model.InvoiceVerifiedEvent;
import com.smartprocure.notification.model.PurchaseOrderIssuedEvent;
import com.smartprocure.notification.model.VendorApprovedEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Per-type Kafka consumer factories.
 *
 * <p>Because the backend publishes without type headers
 * ({@code spring.json.add.type.headers: false}), each consumer
 * factory explicitly sets the target deserialisation type via
 * {@link JsonDeserializer#setDefaultType}.  This avoids any
 * shared state or complex topic-to-class mapping configuration.
 */
@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    // ── Shared base properties ────────────────────────────────────────────────

    private Map<String, Object> baseProps() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        return props;
    }

    private <T> ConsumerFactory<String, T> factory(Class<T> type) {
        JsonDeserializer<T> deser = new JsonDeserializer<>(type);
        deser.addTrustedPackages("*");
        deser.setUseTypeHeaders(false);
        return new DefaultKafkaConsumerFactory<>(baseProps(), new StringDeserializer(), deser);
    }

    private <T> ConcurrentKafkaListenerContainerFactory<String, T> containerFactory(
            ConsumerFactory<String, T> cf) {
        ConcurrentKafkaListenerContainerFactory<String, T> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(cf);
        factory.setConcurrency(3);
        return factory;
    }

    // ── VendorApprovedEvent ───────────────────────────────────────────────────

    @Bean
    public ConsumerFactory<String, VendorApprovedEvent> vendorApprovedConsumerFactory() {
        return factory(VendorApprovedEvent.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, VendorApprovedEvent>
    vendorApprovedListenerFactory() {
        return containerFactory(vendorApprovedConsumerFactory());
    }

    // ── PurchaseOrderIssuedEvent ──────────────────────────────────────────────

    @Bean
    public ConsumerFactory<String, PurchaseOrderIssuedEvent> poIssuedConsumerFactory() {
        return factory(PurchaseOrderIssuedEvent.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PurchaseOrderIssuedEvent>
    poIssuedListenerFactory() {
        return containerFactory(poIssuedConsumerFactory());
    }

    // ── InvoiceVerifiedEvent ──────────────────────────────────────────────────

    @Bean
    public ConsumerFactory<String, InvoiceVerifiedEvent> invoiceVerifiedConsumerFactory() {
        return factory(InvoiceVerifiedEvent.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, InvoiceVerifiedEvent>
    invoiceVerifiedListenerFactory() {
        return containerFactory(invoiceVerifiedConsumerFactory());
    }
}
