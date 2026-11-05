package com.smartprocure.invoice.entity;

import com.smartprocure.common.audit.AuditableEntity;
import com.smartprocure.purchaseorder.entity.PurchaseOrder;
import com.smartprocure.vendor.entity.Vendor;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoices",
        indexes = {
                @Index(name = "idx_inv_po",     columnList = "purchase_order_id"),
                @Index(name = "idx_inv_vendor", columnList = "vendor_id"),
                @Index(name = "idx_inv_status", columnList = "status"),
                @Index(name = "idx_inv_number", columnList = "invoice_number", unique = true)
        })
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Invoice extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @Column(name = "invoice_number", nullable = false, unique = true, length = 60)
    private String invoiceNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvoiceStatus status = InvoiceStatus.SUBMITTED;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, length = 3)
    private String currency = "USD";

    @Column(name = "invoice_date", nullable = false)
    private LocalDate invoiceDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    /** URL or storage key to the uploaded invoice document. */
    @Column(name = "document_url", length = 500)
    private String documentUrl;

    @Column(columnDefinition = "TEXT")
    private String notes;

    /** Reason populated when status = DISPUTED. */
    @Column(name = "dispute_reason", columnDefinition = "TEXT")
    private String disputeReason;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Version
    private Long version;

    // ── Helpers ───────────────────────────────────────────────────────────────

    public boolean isVerifiable() {
        return status == InvoiceStatus.SUBMITTED || status == InvoiceStatus.UNDER_REVIEW;
    }

    public boolean isDisputable() {
        return status == InvoiceStatus.SUBMITTED || status == InvoiceStatus.UNDER_REVIEW;
    }

    public boolean isCancellable() {
        return status == InvoiceStatus.SUBMITTED || status == InvoiceStatus.UNDER_REVIEW
                || status == InvoiceStatus.DISPUTED;
    }
}
