package com.smartprocure.vendor.entity;

/**
 * Vendor lifecycle states.
 *
 * <p>Valid transitions:
 * <pre>
 *   PENDING  → APPROVED  (procurement admin approves)
 *   PENDING  → REJECTED  (procurement admin rejects)
 *   APPROVED → SUSPENDED (procurement admin suspends)
 *   APPROVED → INACTIVE  (procurement admin deactivates)
 *   SUSPENDED→ APPROVED  (procurement admin reactivates)
 *   INACTIVE → APPROVED  (procurement admin reactivates)
 * </pre>
 */
public enum VendorStatus {
    PENDING,    // registered, awaiting procurement approval
    APPROVED,   // active and eligible for quotation requests
    REJECTED,   // not approved — cannot receive RFQs
    SUSPENDED,  // temporarily suspended
    INACTIVE    // soft-deactivated
}
