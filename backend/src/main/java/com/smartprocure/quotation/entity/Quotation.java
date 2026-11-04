package com.smartprocure.quotation.entity;

import com.smartprocure.vendor.entity.Vendor;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quotations",
        indexes = {
                @Index(name = "idx_q_rfq_id",    columnList = "quotation_request_id"),
                @Index(name = "idx_q_vendor_id",  columnList = "vendor_id"),
                @Index(name = "idx_q_status",     columnList = "status")
        })
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Quotation {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quotation_request_id", nullable = false)
    private QuotationRequest quotationRequest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuotationStatus status = QuotationStatus.SUBMITTED;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "USD";

    @Column(name = "validity_date", nullable = false)
    private LocalDate validityDate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "is_selected", nullable = false)
    private boolean selected = false;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "quotation", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<QuotationItem> items = new ArrayList<>();

    @PrePersist
    void onCreate() {
        createdAt = updatedAt = LocalDateTime.now();
        if (submittedAt == null) submittedAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() { updatedAt = LocalDateTime.now(); }

    public boolean isExpired() {
        return validityDate != null && LocalDate.now().isAfter(validityDate);
    }
}
