package com.smartprocure.invoice.repository;

import com.smartprocure.invoice.entity.Invoice;
import com.smartprocure.invoice.entity.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    boolean existsByInvoiceNumber(String invoiceNumber);

    @Query("""
            SELECT i FROM Invoice i
            LEFT JOIN FETCH i.purchaseOrder
            LEFT JOIN FETCH i.vendor
            WHERE i.id = :id
            """)
    Optional<Invoice> findByIdWithDetails(@Param("id") Long id);

    @Query(value = """
            SELECT i FROM Invoice i
            LEFT JOIN FETCH i.purchaseOrder
            LEFT JOIN FETCH i.vendor
            WHERE (:status   IS NULL OR i.status            = :status)
              AND (:vendorId  IS NULL OR i.vendor.id         = :vendorId)
              AND (:poId      IS NULL OR i.purchaseOrder.id  = :poId)
              AND (:keyword   IS NULL
                   OR LOWER(i.invoiceNumber) LIKE LOWER(CONCAT('%',:keyword,'%')))
            """,
           countQuery = """
            SELECT COUNT(i) FROM Invoice i
            WHERE (:status   IS NULL OR i.status            = :status)
              AND (:vendorId  IS NULL OR i.vendor.id         = :vendorId)
              AND (:poId      IS NULL OR i.purchaseOrder.id  = :poId)
              AND (:keyword   IS NULL
                   OR LOWER(i.invoiceNumber) LIKE LOWER(CONCAT('%',:keyword,'%')))
            """)
    Page<Invoice> search(
            @Param("status")   InvoiceStatus status,
            @Param("vendorId") Long vendorId,
            @Param("poId")     Long poId,
            @Param("keyword")  String keyword,
            Pageable pageable);

    long countByPurchaseOrderId(Long purchaseOrderId);
}
