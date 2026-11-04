package com.smartprocure.purchaseorder.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "purchase_order_items",
        indexes = @Index(name = "idx_poi_po_id", columnList = "purchase_order_id"))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PurchaseOrderItem {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    @Column(name = "item_name", nullable = false, length = 255)
    private String itemName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 15, scale = 4)
    private BigDecimal quantity;

    @Column(length = 50)
    private String unit;

    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalPrice;

    @Column(name = "delivered_qty", nullable = false, precision = 15, scale = 4)
    private BigDecimal deliveredQty = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @PrePersist @PreUpdate
    void recalc() {
        if (quantity != null && unitPrice != null)
            totalPrice = quantity.multiply(unitPrice);
    }

    public boolean isFullyDelivered() {
        return deliveredQty != null && deliveredQty.compareTo(quantity) >= 0;
    }
}
