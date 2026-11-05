package com.smartprocure.notification.model;

import java.time.Instant;

/** Local copy of the backend event — decoupled from the backend module. */
public record VendorApprovedEvent(
        String  eventId,
        Instant occurredAt,
        Long    vendorId,
        String  vendorName,
        String  vendorEmail,
        String  approvedBy
) {}
