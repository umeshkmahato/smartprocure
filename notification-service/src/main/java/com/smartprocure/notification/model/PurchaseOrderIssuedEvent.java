package com.smartprocure.notification.model;

import java.math.BigDecimal;
import java.time.Instant;

/** Local copy of the backend event — decoupled from the backend module. */
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
) {}
