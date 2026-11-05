package com.smartprocure.purchaseorder.repository;

import com.smartprocure.purchaseorder.entity.PurchaseOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PurchaseOrderItemRepository extends JpaRepository<PurchaseOrderItem, Long> {

    List<PurchaseOrderItem> findByPurchaseOrderId(Long purchaseOrderId);

    Optional<PurchaseOrderItem> findByIdAndPurchaseOrderId(Long id, Long purchaseOrderId);

    void deleteByPurchaseOrderId(Long purchaseOrderId);
}
