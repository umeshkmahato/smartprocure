package com.smartprocure.approval.entity;

import com.smartprocure.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "approval_delegations",
        indexes = {
                @Index(name = "idx_del_delegator", columnList = "delegator_id"),
                @Index(name = "idx_del_delegate",  columnList = "delegate_id"),
                @Index(name = "idx_del_dates",     columnList = "start_date, end_date")
        })
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ApprovalDelegation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "delegator_id", nullable = false)
    private User delegator;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "delegate_id", nullable = false)
    private User delegate;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }

    @PreUpdate
    void onUpdate() { updatedAt = LocalDateTime.now(); }

    public boolean isActiveOn(LocalDate date) {
        return active && !date.isBefore(startDate) && !date.isAfter(endDate);
    }
}
