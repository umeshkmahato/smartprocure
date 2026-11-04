package com.smartprocure.approval.entity;

import com.smartprocure.purchaserequest.entity.PurchaseRequest;
import com.smartprocure.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "approval_requests",
        indexes = {
                @Index(name = "idx_ar_pr_id",    columnList = "purchase_request_id"),
                @Index(name = "idx_ar_approver", columnList = "approver_id"),
                @Index(name = "idx_ar_status",   columnList = "status"),
                @Index(name = "idx_ar_level",    columnList = "level")
        })
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ApprovalRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_request_id", nullable = false)
    private PurchaseRequest purchaseRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "threshold_id")
    private ApprovalThreshold threshold;

    @Column(name = "level", nullable = false)
    private Integer level;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_id")
    private User approver;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ApprovalStatus status = ApprovalStatus.PENDING;

    @Column(name = "comments", columnDefinition = "TEXT")
    private String comments;

    @Column(name = "actioned_at")
    private LocalDateTime actionedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delegated_to_id")
    private User delegatedTo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }

    @PreUpdate
    void onUpdate() { updatedAt = LocalDateTime.now(); }

    public boolean isPending()   { return status == ApprovalStatus.PENDING; }
    public boolean isActioned()  { return status != ApprovalStatus.PENDING; }
}
