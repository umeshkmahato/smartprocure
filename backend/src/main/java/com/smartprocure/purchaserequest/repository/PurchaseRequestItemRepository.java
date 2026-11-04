package com.smartprocure.purchaserequest.repository;

import com.smartprocure.purchaserequest.entity.PurchaseRequestItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseRequestItemRepository extends JpaRepository<PurchaseRequestItem, Long> {

    List<PurchaseRequestItem> findByPurchaseRequestId(Long purchaseRequestId);

    Optional<PurchaseRequestItem> findByIdAndPurchaseRequestId(Long id, Long purchaseRequestId);

    @Modifying
    @Query("DELETE FROM PurchaseRequestItem i WHERE i.purchaseRequest.id = :prId")
    void deleteAllByPurchaseRequestId(@Param("prId") Long prId);
}
