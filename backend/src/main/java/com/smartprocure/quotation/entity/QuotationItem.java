package com.smartprocure.quotation.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "quotation_items",
        indexes = @Index(name = "idx_qi_quotation_id", columnList = "quotation_id"))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class QuotationItem {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quotation_id", nullable = false)
    private Quotation quotation;

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

    @Column(columnDefinition = "TEXT")
    private String notes;

    @PrePersist @PreUpdate
    void recalc() {
        if (quantity != null && unitPrice != null)
            totalPrice = quantity.multiply(unitPrice);
    }
}
