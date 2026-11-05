package com.smartprocure.purchaseorder.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.smartprocure.purchaseorder.entity.PurchaseOrderItem;
import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PurchaseOrderItemDto(
        Long id, String itemName, String description,
        BigDecimal quantity, String unit,
        BigDecimal unitPrice, BigDecimal totalPrice,
        BigDecimal deliveredQty, boolean fullyDelivered, String notes
) {
    public static PurchaseOrderItemDto fromEntity(PurchaseOrderItem i) {
        return new PurchaseOrderItemDto(
                i.getId(), i.getItemName(), i.getDescription(),
                i.getQuantity(), i.getUnit(),
                i.getUnitPrice(), i.getTotalPrice(),
                i.getDeliveredQty(), i.isFullyDelivered(), i.getNotes());
    }
}
