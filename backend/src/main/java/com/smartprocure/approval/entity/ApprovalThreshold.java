package com.smartprocure.approval.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "approval_thresholds")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ApprovalThreshold {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "level", nullable = false)
    private Integer level;

    @Column(name = "min_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal minAmount;

    @Column(name = "max_amount", precision = 15, scale = 2)
    private BigDecimal maxAmount; // null = no upper limit

    @Column(name = "role_required", nullable = false, length = 100)
    private String roleRequired;

    @Column(name = "description", length = 255)
    private String description;

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

    public boolean matches(BigDecimal amount) {
        if (amount == null) return false;
        boolean aboveMin = amount.compareTo(minAmount) >= 0;
        boolean belowMax = maxAmount == null || amount.compareTo(maxAmount) <= 0;
        return aboveMin && belowMax;
    }
}
