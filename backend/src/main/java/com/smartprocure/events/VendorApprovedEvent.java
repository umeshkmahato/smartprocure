package com.smartprocure.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event published to Kafka topic {@code sp.vendor.approved}
 * when a vendor's status transitions to APPROVED.
 *
 * <p>{@code eventId} is a UUID used for idempotency checks in consumers.
 */
public record VendorApprovedEvent(
        String  eventId,
        Instant occurredAt,
        Long    vendorId,
        String  vendorName,
        String  vendorEmail,
        String  approvedBy
) {
    /** Factory: populates eventId and timestamp automatically. */
    public static VendorApprovedEvent of(Long vendorId,
                                         String vendorName,
                                         String vendorEmail,
                                         String approvedBy) {
        return new VendorApprovedEvent(
                UUID.randomUUID().toString(),
                Instant.now(),
                vendorId,
                vendorName,
                vendorEmail,
                approvedBy
        );
    }
}
