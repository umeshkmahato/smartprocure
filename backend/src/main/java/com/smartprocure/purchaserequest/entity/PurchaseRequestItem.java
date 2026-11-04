package com.smartprocure.purchaserequest.entity;

import com.smartprocure.vendor.entity.Vendor;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "purchase_request_items",
        indexes = {
                @Index(name = "idx_pri_pr_id",  columnList = "purchase_request_id"),
                @Index(name = "idx_pri_vendor", columnList = "vendor_id")
        })
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PurchaseRequestItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_request_id", nullable = false)
    private PurchaseRequest purchaseRequest;

    @Column(name = "item_name", nullable = false, length = 255)
    private String itemName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "quantity", nullable = false, precision = 15, scale = 4)
    private BigDecimal quantity;

    @Column(name = "unit", length = 50)
    private String unit;

    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id")
    private Vendor preferredVendor;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = updatedAt = LocalDateTime.now();
        recalcTotalPrice();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
        recalcTotalPrice();
    }

    public void recalcTotalPrice() {
        if (quantity != null && unitPrice != null) {
            totalPrice = quantity.multiply(unitPrice);
        }
    }
}
