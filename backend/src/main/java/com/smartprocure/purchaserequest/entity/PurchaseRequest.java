package com.smartprocure.purchaserequest.entity;

import com.smartprocure.common.audit.AuditableEntity;
import com.smartprocure.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "purchase_requests",
        indexes = {
                @Index(name = "idx_pr_status",     columnList = "status"),
                @Index(name = "idx_pr_requester",  columnList = "requester_id"),
                @Index(name = "idx_pr_priority",   columnList = "priority"),
                @Index(name = "idx_pr_created_at", columnList = "created_at"),
                @Index(name = "idx_pr_number",     columnList = "pr_number")
        })
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PurchaseRequest extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pr_number", nullable = false, unique = true, length = 30)
    private String prNumber;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PurchaseRequestStatus status = PurchaseRequestStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 10)
    private PurchaseRequestPriority priority = PurchaseRequestPriority.MEDIUM;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @Column(name = "department", length = 100)
    private String department;

    @Column(name = "required_by_date")
    private LocalDate requiredByDate;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "USD";

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Version
    @Column(name = "version")
    private Long version;

    @OneToMany(mappedBy = "purchaseRequest",
               cascade = CascadeType.ALL,
               orphanRemoval = true,
               fetch = FetchType.LAZY)
    @Builder.Default
    private List<PurchaseRequestItem> items = new ArrayList<>();

    // ── helpers ──────────────────────────────────────────────────────────────

    public void addItem(PurchaseRequestItem item) {
        item.setPurchaseRequest(this);
        items.add(item);
        recalcTotal();
    }

    public void removeItem(PurchaseRequestItem item) {
        items.remove(item);
        item.setPurchaseRequest(null);
        recalcTotal();
    }

    public void recalcTotal() {
        totalAmount = items.stream()
                .map(PurchaseRequestItem::getTotalPrice)
                .filter(p -> p != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean isDraft()       { return status == PurchaseRequestStatus.DRAFT; }
    public boolean isSubmitted()   { return status == PurchaseRequestStatus.SUBMITTED; }
    public boolean isCancellable() {
        return status == PurchaseRequestStatus.DRAFT
            || status == PurchaseRequestStatus.SUBMITTED
            || status == PurchaseRequestStatus.UNDER_REVIEW;
    }
}
