package com.smartprocure.quotation.repository;

import com.smartprocure.quotation.entity.Quotation;
import com.smartprocure.quotation.entity.QuotationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuotationRepository extends JpaRepository<Quotation, Long> {

    List<Quotation> findByQuotationRequestIdOrderByTotalAmountAsc(Long rfqId);

    Page<Quotation> findByVendorId(Long vendorId, Pageable pageable);

    Optional<Quotation> findByQuotationRequestIdAndVendorId(Long rfqId, Long vendorId);

    boolean existsByQuotationRequestIdAndVendorId(Long rfqId, Long vendorId);

    @Modifying
    @Query("UPDATE Quotation q SET q.selected = false WHERE q.quotationRequest.id = :rfqId")
    void clearSelectedForRfq(@Param("rfqId") Long rfqId);

    List<Quotation> findByQuotationRequestIdAndStatus(Long rfqId, QuotationStatus status);
}
