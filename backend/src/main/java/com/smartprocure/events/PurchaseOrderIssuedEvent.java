package com.smartprocure.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Domain event published to Kafka topic {@code sp.po.issued}
 * when a Purchase Order transitions from DRAFT → ISSUED.
 */
public record PurchaseOrderIssuedEvent(
        String     eventId,
        Instant    occurredAt,
        Long       poId,
        String     poNumber,
        Long       vendorId,
        String     vendorName,
        String     vendorEmail,
        BigDecimal totalAmount,
        String     currency,
        String     issuedBy
) {
    public static PurchaseOrderIssuedEvent of(Long poId,
                                               String poNumber,
                                               Long vendorId,
                                               String vendorName,
                                               String vendorEmail,
                                               BigDecimal totalAmount,
                                               String currency,
                                               String issuedBy) {
        return new PurchaseOrderIssuedEvent(
                UUID.randomUUID().toString(),
                Instant.now(),
                poId, poNumber,
                vendorId, vendorName, vendorEmail,
                totalAmount, currency, issuedBy
        );
    }
}
