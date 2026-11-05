package com.smartprocure.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Domain event published to Kafka topic {@code sp.invoice.verified}
 * when an invoice transitions to VERIFIED status.
 */
public record InvoiceVerifiedEvent(
        String     eventId,
        Instant    occurredAt,
        Long       invoiceId,
        String     invoiceNumber,
        Long       poId,
        String     poNumber,
        Long       vendorId,
        String     vendorName,
        String     vendorEmail,
        BigDecimal totalAmount,
        String     currency
) {
    public static InvoiceVerifiedEvent of(Long invoiceId,
                                          String invoiceNumber,
                                          Long poId,
                                          String poNumber,
                                          Long vendorId,
                                          String vendorName,
                                          String vendorEmail,
                                          BigDecimal totalAmount,
                                          String currency) {
        return new InvoiceVerifiedEvent(
                UUID.randomUUID().toString(),
                Instant.now(),
                invoiceId, invoiceNumber,
                poId, poNumber,
                vendorId, vendorName, vendorEmail,
                totalAmount, currency
        );
    }
}
