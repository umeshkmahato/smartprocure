package com.smartprocure.approval.repository;

import com.smartprocure.approval.entity.ApprovalThreshold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ApprovalThresholdRepository extends JpaRepository<ApprovalThreshold, Long> {

    List<ApprovalThreshold> findByActiveTrueOrderByLevelAsc();

    @Query("""
            SELECT t FROM ApprovalThreshold t
            WHERE t.active = true
              AND t.minAmount <= :amount
              AND (t.maxAmount IS NULL OR t.maxAmount >= :amount)
            ORDER BY t.level ASC
            """)
    List<ApprovalThreshold> findApplicableThresholds(@Param("amount") BigDecimal amount);
}
