package com.smartprocure.quotation.repository;

import com.smartprocure.quotation.entity.QuotationRequest;
import com.smartprocure.quotation.entity.QuotationRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface QuotationRequestRepository extends JpaRepository<QuotationRequest, Long> {

    boolean existsByRfqNumber(String rfqNumber);

    Page<QuotationRequest> findByStatus(QuotationRequestStatus status, Pageable pageable);

    @Query("""
            SELECT r FROM QuotationRequest r
            WHERE (:status IS NULL OR r.status = :status)
              AND (:search IS NULL
                   OR LOWER(r.title)     LIKE LOWER(CONCAT('%',:search,'%'))
                   OR LOWER(r.rfqNumber) LIKE LOWER(CONCAT('%',:search,'%')))
            """)
    Page<QuotationRequest> search(@Param("status") QuotationRequestStatus status,
                                   @Param("search") String search,
                                   Pageable pageable);
}
