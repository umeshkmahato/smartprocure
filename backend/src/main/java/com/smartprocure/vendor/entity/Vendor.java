package com.smartprocure.vendor.entity;

import com.smartprocure.common.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(
    name = "vendors",
    indexes = {
        @Index(name = "idx_vendors_status",  columnList = "status"),
        @Index(name = "idx_vendors_active",  columnList = "active"),
        @Index(name = "idx_vendors_name",    columnList = "name"),
        @Index(name = "idx_vendors_rating",  columnList = "rating_score")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class Vendor extends AuditableEntity {

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "tax_id", unique = true, length = 50)
    private String taxId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private VendorStatus status = VendorStatus.PENDING;

    @Column(name = "rating_score", nullable = false, precision = 3, scale = 2)
    private BigDecimal ratingScore = BigDecimal.ZERO;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "website", length = 255)
    private String website;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    // ── Convenience predicates ─────────────────────────────────────────────────
    public boolean isApproved() { return status == VendorStatus.APPROVED; }
    public boolean isPending()  { return status == VendorStatus.PENDING; }
}
