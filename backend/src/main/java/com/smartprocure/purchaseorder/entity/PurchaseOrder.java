package com.smartprocure.purchaseorder.entity;

import com.smartprocure.common.audit.AuditableEntity;
import com.smartprocure.purchaserequest.entity.PurchaseRequest;
import com.smartprocure.quotation.entity.Quotation;
import com.smartprocure.user.entity.User;
import com.smartprocure.vendor.entity.Vendor;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "purchase_orders",
        indexes = {
                @Index(name = "idx_po_status", columnList = "status"),
                @Index(name = "idx_po_vendor", columnList = "vendor_id"),
                @Index(name = "idx_po_pr_id",  columnList = "purchase_request_id"),
                @Index(name = "idx_po_number", columnList = "po_number")
        })
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PurchaseOrder extends AuditableEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "po_number", nullable = false, unique = true, length = 30)
    private String poNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_request_id")
    private PurchaseRequest purchaseRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quotation_id")
    private Quotation quotation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private PurchaseOrderStatus status = PurchaseOrderStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issued_by_id")
    private User issuedBy;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(nullable = false, length = 3)
    private String currency = "USD";

    @Column(name = "delivery_date")
    private LocalDate deliveryDate;

    @Column(name = "delivery_address", columnDefinition = "TEXT")
    private String deliveryAddress;

    @Column(name = "payment_terms", length = 255)
    private String paymentTerms;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Version
    private Long version;

    @Column(name = "issued_at")
    private LocalDateTime issuedAt;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<PurchaseOrderItem> items = new ArrayList<>();

    public void addItem(PurchaseOrderItem item) {
        item.setPurchaseOrder(this);
        items.add(item);
        recalcTotal();
    }

    public void recalcTotal() {
        totalAmount = items.stream()
                .map(PurchaseOrderItem::getTotalPrice)
                .filter(p -> p != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean isDraft()      { return status == PurchaseOrderStatus.DRAFT; }
    public boolean isIssued()     { return status == PurchaseOrderStatus.ISSUED; }
    public boolean isCancellable(){ return status == PurchaseOrderStatus.DRAFT || status == PurchaseOrderStatus.ISSUED; }

    public boolean isFullyDelivered() {
        return items.stream().allMatch(PurchaseOrderItem::isFullyDelivered);
    }
}
