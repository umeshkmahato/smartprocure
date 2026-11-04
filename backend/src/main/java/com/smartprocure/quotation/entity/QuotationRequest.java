package com.smartprocure.quotation.entity;

import com.smartprocure.common.audit.AuditableEntity;
import com.smartprocure.purchaserequest.entity.PurchaseRequest;
import com.smartprocure.user.entity.User;
import com.smartprocure.vendor.entity.Vendor;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quotation_requests",
        indexes = {
                @Index(name = "idx_qr_status", columnList = "status"),
                @Index(name = "idx_qr_pr_id",  columnList = "purchase_request_id")
        })
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class QuotationRequest extends AuditableEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rfq_number", nullable = false, unique = true, length = 30)
    private String rfqNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_request_id")
    private PurchaseRequest purchaseRequest;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuotationRequestStatus status = QuotationRequestStatus.DRAFT;

    @Column(nullable = false)
    private LocalDateTime deadline;

    @Column(nullable = false, length = 3)
    private String currency = "USD";

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User createdByUser;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "quotation_request_vendors",
            joinColumns = @JoinColumn(name = "quotation_request_id"),
            inverseJoinColumns = @JoinColumn(name = "vendor_id"))
    @Builder.Default
    private List<Vendor> invitedVendors = new ArrayList<>();

    @OneToMany(mappedBy = "quotationRequest", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Quotation> quotations = new ArrayList<>();

    public boolean isDraft()     { return status == QuotationRequestStatus.DRAFT; }
    public boolean isPublished() { return status == QuotationRequestStatus.PUBLISHED; }
    public boolean isClosed()    { return status == QuotationRequestStatus.CLOSED; }
}
