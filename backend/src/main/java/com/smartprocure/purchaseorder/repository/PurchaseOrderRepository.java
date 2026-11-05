package com.smartprocure.purchaseorder.repository;

import com.smartprocure.purchaseorder.entity.PurchaseOrder;
import com.smartprocure.purchaseorder.entity.PurchaseOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    @Query("""
            SELECT po FROM PurchaseOrder po
            LEFT JOIN FETCH po.vendor
            LEFT JOIN FETCH po.issuedBy
            WHERE po.id = :id
            """)
    Optional<PurchaseOrder> findByIdWithDetails(@Param("id") Long id);

    @Query("""
            SELECT po FROM PurchaseOrder po
            LEFT JOIN FETCH po.vendor
            LEFT JOIN FETCH po.issuedBy
            WHERE po.poNumber = :poNumber
            """)
    Optional<PurchaseOrder> findByPoNumber(@Param("poNumber") String poNumber);

    boolean existsByPoNumber(String poNumber);

    @Query(value = """
            SELECT po FROM PurchaseOrder po
            LEFT JOIN FETCH po.vendor
            LEFT JOIN FETCH po.issuedBy
            WHERE (:status   IS NULL OR po.status    = :status)
              AND (:vendorId  IS NULL OR po.vendor.id = :vendorId)
              AND (:keyword   IS NULL
                   OR LOWER(po.poNumber) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """,
           countQuery = """
            SELECT COUNT(po) FROM PurchaseOrder po
            WHERE (:status   IS NULL OR po.status    = :status)
              AND (:vendorId  IS NULL OR po.vendor.id = :vendorId)
              AND (:keyword   IS NULL
                   OR LOWER(po.poNumber) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<PurchaseOrder> search(
            @Param("status")   PurchaseOrderStatus status,
            @Param("vendorId") Long vendorId,
            @Param("keyword")  String keyword,
            Pageable pageable);

    long countByVendorId(Long vendorId);
}
