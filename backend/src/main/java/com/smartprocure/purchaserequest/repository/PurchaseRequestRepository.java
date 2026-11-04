package com.smartprocure.purchaserequest.repository;

import com.smartprocure.purchaserequest.entity.PurchaseRequest;
import com.smartprocure.purchaserequest.entity.PurchaseRequestPriority;
import com.smartprocure.purchaserequest.entity.PurchaseRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PurchaseRequestRepository extends JpaRepository<PurchaseRequest, Long> {

    boolean existsByPrNumber(String prNumber);

    Optional<PurchaseRequest> findByPrNumber(String prNumber);

    Page<PurchaseRequest> findByRequesterId(Long requesterId, Pageable pageable);

    @Query("""
            SELECT pr FROM PurchaseRequest pr
            WHERE (:status   IS NULL OR pr.status   = :status)
              AND (:priority  IS NULL OR pr.priority = :priority)
              AND (:requesterId IS NULL OR pr.requester.id = :requesterId)
              AND (:search IS NULL
                   OR LOWER(pr.title)    LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(pr.prNumber) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<PurchaseRequest> search(
            @Param("status")      PurchaseRequestStatus   status,
            @Param("priority")    PurchaseRequestPriority priority,
            @Param("requesterId") Long                    requesterId,
            @Param("search")      String                  search,
            Pageable pageable);
}
