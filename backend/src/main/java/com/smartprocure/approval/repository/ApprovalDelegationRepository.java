package com.smartprocure.approval.repository;

import com.smartprocure.approval.entity.ApprovalDelegation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApprovalDelegationRepository extends JpaRepository<ApprovalDelegation, Long> {

    List<ApprovalDelegation> findByDelegatorId(Long delegatorId);

    @Query("""
            SELECT d FROM ApprovalDelegation d
            WHERE d.delegator.id = :delegatorId
              AND d.active = true
              AND d.startDate <= :today
              AND d.endDate   >= :today
            """)
    Optional<ApprovalDelegation> findActiveDelegation(
            @Param("delegatorId") Long      delegatorId,
            @Param("today")       LocalDate today);

    @Query("""
            SELECT CASE WHEN COUNT(d) > 0 THEN true ELSE false END
            FROM ApprovalDelegation d
            WHERE d.delegator.id = :delegatorId
              AND d.active = true
              AND d.startDate <= :endDate
              AND d.endDate   >= :startDate
            """)
    boolean hasOverlappingDelegation(
            @Param("delegatorId") Long      delegatorId,
            @Param("startDate")   LocalDate startDate,
            @Param("endDate")     LocalDate endDate);
}
