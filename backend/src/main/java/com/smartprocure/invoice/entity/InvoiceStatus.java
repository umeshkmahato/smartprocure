package com.smartprocure.invoice.entity;

public enum InvoiceStatus {
    SUBMITTED,      // vendor has uploaded the invoice
    UNDER_REVIEW,   // procurement officer is verifying
    VERIFIED,       // amounts match PO; ready for payment
    DISPUTED,       // mismatch found; returned to vendor
    PAID,           // payment confirmed
    CANCELLED       // withdrawn before payment
}
