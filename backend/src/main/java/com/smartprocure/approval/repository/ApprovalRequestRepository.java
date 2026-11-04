package com.smartprocure.approval.repository;

import com.smartprocure.approval.entity.ApprovalRequest;
import com.smartprocure.approval.entity.ApprovalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, Long> {

    List<ApprovalRequest> findByPurchaseRequestIdOrderByLevelAsc(Long purchaseRequestId);

    Optional<ApprovalRequest> findByPurchaseRequestIdAndLevel(Long purchaseRequestId, Integer level);

    List<ApprovalRequest> findByPurchaseRequestIdAndStatus(Long purchaseRequestId, ApprovalStatus status);

    Page<ApprovalRequest> findByApproverIdAndStatus(Long approverId, ApprovalStatus status, Pageable pageable);

    Page<ApprovalRequest> findByApproverId(Long approverId, Pageable pageable);

    boolean existsByPurchaseRequestIdAndStatus(Long purchaseRequestId, ApprovalStatus status);

    @Query("""
            SELECT ar FROM ApprovalRequest ar
            WHERE ar.approver.id = :approverId
              AND ar.status = 'PENDING'
            ORDER BY ar.createdAt ASC
            """)
    Page<ApprovalRequest> findPendingForApprover(@Param("approverId") Long approverId, Pageable pageable);

    @Query("""
            SELECT COUNT(ar) FROM ApprovalRequest ar
            WHERE ar.purchaseRequest.id = :prId
              AND ar.status = 'APPROVED'
            """)
    long countApprovedSteps(@Param("prId") Long prId);

    @Query("""
            SELECT COUNT(ar) FROM ApprovalRequest ar
            WHERE ar.purchaseRequest.id = :prId
            """)
    long countTotalSteps(@Param("prId") Long prId);
}
